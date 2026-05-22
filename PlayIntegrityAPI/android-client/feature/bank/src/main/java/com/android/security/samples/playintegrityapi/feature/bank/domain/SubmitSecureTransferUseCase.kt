package com.android.security.samples.playintegrityapi.feature.bank.domain

import android.util.Log
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.Utils.generateSha256Hash
import com.android.security.samples.playintegrityapi.feature.bank.data.remote.TransferErrorResponse
import com.android.security.samples.playintegrityapi.feature.bank.data.remote.TransferRequest
import com.android.security.samples.playintegrityapi.feature.bank.data.repository.BankRepository
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

sealed interface TransferResult {
    data class Success(val transactionId: String, val message: String) : TransferResult

    sealed interface Failure : TransferResult {
        data class NetworkError(val message: String) : Failure

        sealed interface IntegrityError : Failure {
            val message: String

            data class Server(
                override val message: String,
                val remediationDialogTypeCode: Int?,
                val token: StandardIntegrityToken
            ) : IntegrityError

            data class Client(
                override val message: String,
                val exception: StandardIntegrityException?
            ) : IntegrityError
        }
    }
}

class SubmitSecureTransferUseCase @Inject constructor(
    private val bankRepository: BankRepository,
    private val integrityRepository: IntegrityRepository,
    private val moshi: Moshi
) {
    private val requestAdapter = moshi.adapter(TransferRequest::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    private companion object {
        const val TAG = "SecureTransferUseCase"
    }

    suspend operator fun invoke(accountNumber: String, amount: BigDecimal): TransferResult {
        Log.d(TAG, "--- Initiating Secure Transfer ---")
        Log.d(TAG, "Amount: $amount, Account: ${accountNumber.takeLast(4).padStart(accountNumber.length, '*')}")

        val request = createTransferRequest(accountNumber, amount)
        val requestHash = generateContentBindingHash(request)
            ?: return TransferResult.Failure.NetworkError("Internal data processing error")

        val tokenResult = fetchIntegrityToken(requestHash)
        if (tokenResult.isFailure) {
            return handleTokenFailure(tokenResult.exceptionOrNull())
        }

        val integrityToken = tokenResult.getOrThrow()
        return executeAndProcessTransfer(request, integrityToken)
    }

    private fun createTransferRequest(accountNumber: String, amount: BigDecimal): TransferRequest {
        return TransferRequest(
            amount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            accountNumber = accountNumber
        )
    }

    private fun generateContentBindingHash(request: TransferRequest): String? {
        return try {
            val rawJson = requestAdapter.toJson(request)
            val map = mapAdapter.fromJson(rawJson) ?: return null
            val canonicalJson = mapAdapter.toJson(map.toSortedMap())
            val hash = generateSha256Hash(canonicalJson)
            Log.d(TAG, "1. Content-binding hash generated: $hash")
            hash
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize transfer request", e)
            null
        }
    }

    private suspend fun fetchIntegrityToken(requestHash: String): Result<StandardIntegrityToken> {
        Log.d(TAG, "2. Requesting Standard Integrity Token from Play Core...")
        return integrityRepository.requestIntegrityToken(requestHash).onSuccess { token ->
            Log.d(TAG, "-> Token successfully retrieved. Length: ${token.token().length}")
        }
    }

    private fun handleTokenFailure(throwable: Throwable?): TransferResult.Failure.IntegrityError.Client {
        val exception = throwable as? StandardIntegrityException
        Log.e(
            TAG,
            "-> Play Integrity Token generation failed locally! Code: ${exception?.errorCode}",
            exception
        )

        return TransferResult.Failure.IntegrityError.Client(
            message = "Device integrity check failed locally.",
            exception = exception
        )
    }

    private suspend fun executeAndProcessTransfer(
        request: TransferRequest,
        integrityToken: StandardIntegrityToken
    ): TransferResult {
        Log.d(TAG, "Submitting transaction to Bank Server...")

        val response = try {
            bankRepository.submitTransfer(integrityToken.token(), request)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            Log.e(TAG, "!!! Network exception during transfer !!!", e)
            return TransferResult.Failure.NetworkError(e.localizedMessage ?: "Network connection failed")
        }

        if (response.isSuccessful && response.body() != null) {
            val transactionId = response.body()!!.transactionId ?: "UNKNOWN"
            Log.d(TAG, "-> Server SUCCESS! HTTP ${response.code()} | Transaction ID: $transactionId")

            return TransferResult.Success(
                transactionId = transactionId,
                message = response.body()!!.message
            )
        }

        return handleServerFailure(
            responseCode = response.code(),
            errorJson = response.errorBody()?.string(),
            integrityToken = integrityToken
        )
    }

    private fun handleServerFailure(
        responseCode: Int,
        errorJson: String?,
        integrityToken: StandardIntegrityToken
    ): TransferResult.Failure.IntegrityError.Server {
        Log.e(TAG, "-> Server REJECTED request! HTTP $responseCode")
        Log.e(TAG, "-> Raw Error Body: $errorJson")

        var parsedRemediationCode: Int? = null
        var parsedErrorMessage = "Server rejected the transaction."

        if (!errorJson.isNullOrBlank()) {
            try {
                val errorAdapter = moshi.adapter(TransferErrorResponse::class.java)
                val errorResponse = errorAdapter.fromJson(errorJson)
                parsedRemediationCode = errorResponse?.remediationCode
                parsedErrorMessage = errorResponse?.message ?: parsedErrorMessage

                Log.d(TAG, "-> Parsed Remediation Code: $parsedRemediationCode")
            } catch (e: Exception) {
                Log.e(TAG, "-> Failed to parse error response JSON", e)
            }
        }

        return TransferResult.Failure.IntegrityError.Server(
            message = parsedErrorMessage,
            remediationDialogTypeCode = parsedRemediationCode,
            token = integrityToken
        )
    }
}