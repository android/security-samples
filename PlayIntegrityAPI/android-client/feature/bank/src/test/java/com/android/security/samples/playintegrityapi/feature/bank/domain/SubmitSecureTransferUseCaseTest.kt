package com.android.security.samples.playintegrityapi.feature.bank.domain

import android.util.Base64
import android.util.Log
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.feature.bank.data.remote.TransferErrorResponse
import com.android.security.samples.playintegrityapi.feature.bank.data.remote.TransferRequest
import com.android.security.samples.playintegrityapi.feature.bank.data.remote.TransferResponse
import com.android.security.samples.playintegrityapi.feature.bank.data.repository.BankRepository
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Response
import java.math.BigDecimal

class SubmitSecureTransferUseCaseTest {

    private lateinit var bankRepository: BankRepository
    private lateinit var integrityRepository: IntegrityRepository
    private lateinit var useCase: SubmitSecureTransferUseCase
    private lateinit var moshi: Moshi
    private lateinit var requestAdapter: JsonAdapter<TransferRequest>
    private lateinit var errorAdapter: JsonAdapter<TransferErrorResponse>
    private lateinit var mapAdapter: JsonAdapter<Map<String, Any>>

    private lateinit var logMock: MockedStatic<Log>
    private lateinit var base64Mock: MockedStatic<Base64>

    @Before
    fun setup() {
        bankRepository = mock()
        integrityRepository = mock()
        moshi = mock()
        requestAdapter = mock()
        errorAdapter = mock()
        mapAdapter = mock()

        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)

        whenever(moshi.adapter(TransferRequest::class.java)).thenReturn(requestAdapter)
        whenever(moshi.adapter(TransferErrorResponse::class.java)).thenReturn(errorAdapter)
        whenever(moshi.adapter<Map<String, Any>>(mapType)).thenReturn(mapAdapter)

        whenever(requestAdapter.toJson(anyOrNull())).thenReturn("mock_json_payload")
        whenever(mapAdapter.fromJson(anyOrNull<String>())).thenReturn(mapOf("mock" to "data"))
        whenever(mapAdapter.toJson(anyOrNull<Map<String, Any>>())).thenReturn("mock_canonical_json")

        useCase = SubmitSecureTransferUseCase(bankRepository, integrityRepository, moshi)

        logMock = mockStatic(Log::class.java)
        logMock.`when`<Int> { Log.d(anyOrNull(), anyOrNull()) }.thenReturn(0)
        logMock.`when`<Int> { Log.e(anyOrNull(), anyOrNull()) }.thenReturn(0)
        logMock.`when`<Int> { Log.e(anyOrNull(), anyOrNull(), anyOrNull()) }.thenReturn(0)

        base64Mock = mockStatic(Base64::class.java)
        base64Mock.`when`<String> {
            Base64.encodeToString(anyOrNull(), any())
        }.thenReturn("mock_hashed_payload")
    }

    @After
    fun tearDown() {
        logMock.close()
        base64Mock.close()
    }

    @Test
    fun `invoke returns Success when token is fetched and server accepts transfer`() = runTest {
        val mockToken = mock<StandardIntegrityToken>()
        whenever(mockToken.token()).thenReturn("valid_token_string")
        whenever(integrityRepository.requestIntegrityToken("mock_hashed_payload"))
            .thenReturn(Result.success(mockToken))
        val successResponse = Response.success(
            TransferResponse(status = "SUCCESS", transactionId = "TXN-12345", message = "Transfer Complete")
        )
        whenever(bankRepository.submitTransfer(anyOrNull(), anyOrNull()))
            .thenReturn(successResponse)

        val result = useCase(accountNumber = "1234567890", amount = BigDecimal("50.0"))

        assertTrue(result is TransferResult.Success)
        val successResult = result as TransferResult.Success
        assertEquals("TXN-12345", successResult.transactionId)
        assertEquals("Transfer Complete", successResult.message)
    }

    @Test
    fun `invoke returns Client IntegrityError when local Play Core token generation fails`() = runTest {
        val exception = mock<StandardIntegrityException>()
        whenever(exception.errorCode).thenReturn(-1)
        whenever(integrityRepository.requestIntegrityToken(anyOrNull()))
            .thenReturn(Result.failure(exception))

        val result = useCase(accountNumber = "1234567890", amount = BigDecimal("50.0"))

        assertTrue(result is TransferResult.Failure.IntegrityError.Client)
        val clientError = result as TransferResult.Failure.IntegrityError.Client
        assertEquals("Device integrity check failed locally.", clientError.message)
        assertEquals(exception, clientError.exception)
    }

    @Test
    fun `invoke returns Server IntegrityError with parsed remediation code on 403 response`() = runTest {
        val mockToken = mock<StandardIntegrityToken>()
        val mediaType = MediaType.parse("application/json")
        val errorResponseBody = ResponseBody.create(mediaType, "dummy_error_json")
        val errorResponse = Response.error<TransferResponse>(403, errorResponseBody)
        whenever(mockToken.token()).thenReturn("rejected_token_string")
        whenever(integrityRepository.requestIntegrityToken(anyOrNull()))
            .thenReturn(Result.success(mockToken))
        whenever(bankRepository.submitTransfer(anyOrNull(), anyOrNull()))
            .thenReturn(errorResponse)
        val parsedError = TransferErrorResponse(
            status = "ERROR",
            errorCode = "INTEGRITY_REJECTED",
            message = "Device does not meet standards.",
            remediationCode = 4,
            remediationAction = "GET_INTEGRITY"
        )
        whenever(errorAdapter.fromJson(anyOrNull<String>())).thenReturn(parsedError)

        val result = useCase(accountNumber = "1234567890", amount = BigDecimal("50.0"))

        assertTrue(result is TransferResult.Failure.IntegrityError.Server)
        val serverError = result as TransferResult.Failure.IntegrityError.Server
        assertEquals("Device does not meet standards.", serverError.message)
        assertEquals(4, serverError.remediationDialogTypeCode)
        assertEquals(mockToken, serverError.token)
    }

    @Test
    fun `invoke returns Server IntegrityError with default message when server JSON is unparseable`() = runTest {
        val mockToken = mock<StandardIntegrityToken>()
        whenever(mockToken.token()).thenReturn("rejected_token_string")
        whenever(integrityRepository.requestIntegrityToken(anyOrNull()))
            .thenReturn(Result.success(mockToken))
        val mediaType = MediaType.parse("text/plain")
        val errorResponseBody = ResponseBody.create(mediaType, "Gateway Timeout")
        val errorResponse = Response.error<TransferResponse>(504, errorResponseBody)
        whenever(bankRepository.submitTransfer(anyOrNull(), anyOrNull())).thenReturn(errorResponse)
        whenever(errorAdapter.fromJson(anyOrNull<String>())).thenThrow(RuntimeException("Invalid JSON"))

        val result = useCase(accountNumber = "1234567890", amount = BigDecimal("50.0"))

        assertTrue(result is TransferResult.Failure.IntegrityError.Server)
        val serverError = result as TransferResult.Failure.IntegrityError.Server
        assertEquals("Server rejected the transaction.", serverError.message)
        assertEquals(null, serverError.remediationDialogTypeCode)
    }

    @Test
    fun `invoke returns NetworkError when a hard exception is thrown`() = runTest {
        val mockToken = mock<StandardIntegrityToken>()
        whenever(mockToken.token()).thenReturn("valid_token_string")
        whenever(integrityRepository.requestIntegrityToken(any()))
            .thenReturn(Result.success(mockToken))
        whenever(bankRepository.submitTransfer(any(), any()))
            .thenThrow(RuntimeException("No internet connection"))

        val result = useCase(accountNumber = "1234567890", amount = BigDecimal("50.0"))

        assertTrue(result is TransferResult.Failure.NetworkError)
        assertEquals("No internet connection", (result as TransferResult.Failure.NetworkError).message)
    }
}