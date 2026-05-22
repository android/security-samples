package com.android.security.samples.playintegrityapi.feature.bank.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.di.GoogleCloudProjectNumber
import com.android.security.samples.playintegrityapi.feature.bank.domain.SubmitSecureTransferUseCase
import com.android.security.samples.playintegrityapi.feature.bank.domain.TransferResult
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

sealed interface TransferUiState {
    object Idle : TransferUiState
    object Loading : TransferUiState
    object Success : TransferUiState

    sealed interface Error : TransferUiState {
        val message: String

        data class Network(override val message: String) : Error

        sealed interface Integrity : Error {
            val isRemediable: Boolean
            val remediationDialogTypeCode: Int?

            data class Server(
                override val message: String,
                override val remediationDialogTypeCode: Int?,
                val standardIntegrityToken: StandardIntegrityToken
            ) : Integrity {
                override val isRemediable: Boolean
                    get() = remediationDialogTypeCode != null
            }

            data class Client(
                override val message: String,
                val remediationException: StandardIntegrityException?
            ) : Integrity {
                override val isRemediable: Boolean
                    get() = remediationException?.isRemediable == true

                // If client error is remediable, use GET_INTEGRITY (typeCode: 4) dialog to fix
                override val remediationDialogTypeCode: Int? = if (isRemediable) 4 else null
            }
        }
    }
}

data class BankUiState(
    val accountNumber: String = "",
    val amount: String = "",
    val transferState: TransferUiState = TransferUiState.Idle
) {
    val isTransferEnabled: Boolean
        get() {
            val isAccountValid = accountNumber.length >= 9
            val isAmountValid = (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO
            return isAccountValid && isAmountValid && transferState !is TransferUiState.Loading
        }
}

@HiltViewModel
class BankViewModel @Inject constructor(
    private val submitSecureTransferUseCase: SubmitSecureTransferUseCase,
    private val integrityRepository: IntegrityRepository,
    @GoogleCloudProjectNumber private val gcpProjectNumber: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(BankUiState())
    val uiState: StateFlow<BankUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            integrityRepository.warmUp()
        }
    }

    fun updateAccountNumber(number: String) {
        val digitsOnly = number.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                accountNumber = digitsOnly,
                transferState = TransferUiState.Idle
            )
        }
    }

    fun updateAmount(amount: String) {
        _uiState.update {
            it.copy(
                amount = amount,
                transferState = TransferUiState.Idle
            )
        }
    }

    fun transfer() {
        val currentState = _uiState.value
        if (!currentState.isTransferEnabled) return

        viewModelScope.launch {
            _uiState.update { it.copy(transferState = TransferUiState.Loading) }

            val parsedAmount = currentState.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val result = submitSecureTransferUseCase(currentState.accountNumber, parsedAmount)

            handleTransferResult(result)
        }
    }

    fun triggerRemediationDialog(activity: Activity) {
        val state = uiState.value.transferState

        if (state !is TransferUiState.Error.Integrity) return
        val typeCode = state.remediationDialogTypeCode ?: return

        viewModelScope.launch {
            executeRemediationDialog(activity, state, typeCode)
            clearRemediationState()
        }
    }

    fun clearRemediationState() {
        _uiState.update { it.copy(transferState = TransferUiState.Idle) }
    }

    private fun handleTransferResult(result: TransferResult) {
        when (result) {
            is TransferResult.Success -> handleTransferSuccess()
            is TransferResult.Failure.IntegrityError.Server -> handleServerIntegrityError(result)
            is TransferResult.Failure.IntegrityError.Client -> handleClientIntegrityError(result)
            is TransferResult.Failure.NetworkError -> handleNetworkError(result)
        }
    }

    private fun handleTransferSuccess() {
        _uiState.update { it.copy(transferState = TransferUiState.Success) }
    }

    private fun handleServerIntegrityError(result: TransferResult.Failure.IntegrityError.Server) {
        _uiState.update {
            it.copy(
                transferState = TransferUiState.Error.Integrity.Server(
                    message = result.message,
                    remediationDialogTypeCode = result.remediationDialogTypeCode,
                    standardIntegrityToken = result.token
                )
            )
        }
    }

    private fun handleClientIntegrityError(result: TransferResult.Failure.IntegrityError.Client) {
        _uiState.update {
            it.copy(
                transferState = TransferUiState.Error.Integrity.Client(
                    message = result.message,
                    remediationException = result.exception
                )
            )
        }
    }

    private fun handleNetworkError(result: TransferResult.Failure.NetworkError) {
        _uiState.update {
            it.copy(
                transferState = TransferUiState.Error.Network(
                    message = result.message
                )
            )
        }
    }

    private suspend fun executeRemediationDialog(
        activity: Activity,
        state: TransferUiState.Error.Integrity,
        typeCode: Int
    ) {
        when (state) {
            is TransferUiState.Error.Integrity.Server -> {
                integrityRepository.showDialog(activity, typeCode, state.standardIntegrityToken)
            }
            is TransferUiState.Error.Integrity.Client -> {
                state.remediationException?.let {
                    integrityRepository.showDialog(activity, typeCode, it)
                }
            }
        }
    }
}