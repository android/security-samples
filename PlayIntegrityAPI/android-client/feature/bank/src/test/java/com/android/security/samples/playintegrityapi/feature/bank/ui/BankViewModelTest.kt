// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.android.security.samples.playintegrityapi.feature.bank.ui

import android.app.Activity
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.feature.bank.domain.SubmitSecureTransferUseCase
import com.android.security.samples.playintegrityapi.feature.bank.domain.TransferResult
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BankViewModelTest {
    private lateinit var submitSecureTransferUseCase: SubmitSecureTransferUseCase
    private lateinit var integrityRepository: IntegrityRepository
    private lateinit var viewModel: BankViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        submitSecureTransferUseCase = mock()
        integrityRepository = mock()

        viewModel = BankViewModel(submitSecureTransferUseCase, integrityRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls warmUp on IntegrityRepository`() = runTest {
        verify(integrityRepository).warmUp()
    }

    @Test
    fun `updateAccountNumber strips non-digits and sets state to Idle`() {
        viewModel.clearRemediationState()
        viewModel.updateAccountNumber("12A-34B-56C789")

        val state = viewModel.uiState.value
        assertEquals("123456789", state.accountNumber)
        assertEquals(TransferUiState.Idle, state.transferState)
    }

    @Test
    fun `updateAmount updates value and sets state to Idle`() {
        viewModel.updateAmount("150.50")

        val state = viewModel.uiState.value
        assertEquals("150.50", state.amount)
        assertEquals(TransferUiState.Idle, state.transferState)
    }

    @Test
    fun `isTransferEnabled evaluates correctly based on input rules`() {
        assertFalse(viewModel.uiState.value.isTransferEnabled)

        // Invalid account length (< 9)
        viewModel.updateAccountNumber("12345678")
        viewModel.updateAmount("50.0")
        assertFalse(viewModel.uiState.value.isTransferEnabled)

        // Invalid amount (0)
        viewModel.updateAccountNumber("123456789")
        viewModel.updateAmount("0.0")
        assertFalse(viewModel.uiState.value.isTransferEnabled)

        // Valid inputs
        viewModel.updateAmount("50.0")
        assertTrue(viewModel.uiState.value.isTransferEnabled)
    }

    @Test
    fun `transfer does nothing if isTransferEnabled is false`() = runTest {
        viewModel.transfer()

        verifyNoInteractions(submitSecureTransferUseCase)
    }

    @Test
    fun `transfer maps Success result correctly`() = runTest {
        setupValidFormInputs()
        whenever(submitSecureTransferUseCase(any(), any())).thenReturn(
            TransferResult.Success("TXN-123", "Transfer Complete")
        )

        viewModel.transfer()

        assertEquals(TransferUiState.Success, viewModel.uiState.value.transferState)
    }

    @Test
    fun `transfer maps NetworkError result correctly`() = runTest {
        setupValidFormInputs()
        whenever(submitSecureTransferUseCase(any(), any())).thenReturn(
            TransferResult.Failure.NetworkError("No internet")
        )

        viewModel.transfer()

        val transferState = viewModel.uiState.value.transferState
        assertTrue(transferState is TransferUiState.Error.Network)
        assertEquals("No internet", (transferState as TransferUiState.Error.Network).message)
    }

    @Test
    fun `transfer maps Server Integrity Error correctly`() = runTest {
        setupValidFormInputs()
        val mockToken = mock<StandardIntegrityToken>()
        whenever(submitSecureTransferUseCase(any(), any())).thenReturn(
            TransferResult.Failure.IntegrityError.Server(
                message = "Device rejected",
                remediationDialogTypeCode = 4,
                token = mockToken
            )
        )

        viewModel.transfer()

        val transferState = viewModel.uiState.value.transferState
        assertTrue(transferState is TransferUiState.Error.Integrity.Server)
        val serverError = transferState as TransferUiState.Error.Integrity.Server
        assertEquals("Device rejected", serverError.message)
        assertEquals(4, serverError.remediationDialogTypeCode)
        assertTrue(serverError.isRemediable)
        assertEquals(mockToken, serverError.standardIntegrityToken)
    }

    @Test
    fun `transfer maps Client Integrity Error correctly`() = runTest {
        setupValidFormInputs()
        val mockException = mock<StandardIntegrityException>()
        whenever(mockException.isRemediable).thenReturn(true)
        whenever(submitSecureTransferUseCase(any(), any())).thenReturn(
            TransferResult.Failure.IntegrityError.Client(
                message = "Local Play Core failed",
                exception = mockException
            )
        )

        viewModel.transfer()

        val transferState = viewModel.uiState.value.transferState
        assertTrue(transferState is TransferUiState.Error.Integrity.Client)
        val clientError = transferState as TransferUiState.Error.Integrity.Client
        assertEquals("Local Play Core failed", clientError.message)
        assertTrue(clientError.isRemediable)
        assertEquals(4, clientError.remediationDialogTypeCode)
        assertEquals(mockException, clientError.remediationException)
    }

    @Test
    fun `triggerRemediationDialog for Server error calls repository and clears state`() = runTest {
        setupValidFormInputs()
        val mockToken = mock<StandardIntegrityToken>()
        whenever(submitSecureTransferUseCase(any(), any())).thenReturn(
            TransferResult.Failure.IntegrityError.Server(
                message = "Error",
                remediationDialogTypeCode = 4,
                token = mockToken
            )
        )
        val mockActivity = mock<Activity>()

        viewModel.transfer()
        viewModel.triggerRemediationDialog(mockActivity)

        verify(integrityRepository).showDialog(eq(mockActivity), eq(4), eq(mockToken))
        assertEquals(TransferUiState.Idle, viewModel.uiState.value.transferState) // Ensures state was cleared
    }

    @Test
    fun `triggerRemediationDialog for Client error calls repository and clears state`() = runTest {
        setupValidFormInputs()
        val mockException = mock<StandardIntegrityException>()
        whenever(mockException.isRemediable).thenReturn(true)
        whenever(submitSecureTransferUseCase(any(), any())).thenReturn(
            TransferResult.Failure.IntegrityError.Client(
                message = "Error",
                exception = mockException
            )
        )
        val mockActivity = mock<Activity>()

        viewModel.transfer()
        viewModel.triggerRemediationDialog(mockActivity)

        verify(integrityRepository).showDialog(eq(mockActivity), eq(4), eq(mockException))
        assertEquals(TransferUiState.Idle, viewModel.uiState.value.transferState)
    }

    private fun setupValidFormInputs() {
        viewModel.updateAccountNumber("123456789")
        viewModel.updateAmount("50.0")
    }

    private companion object {
        const val TEST_CLOUD_PROJECT_NUMBER = 12345678910L
    }
}