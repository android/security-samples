package com.android.security.samples.playintegrityapi.feature.game.domain

import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStopRequest
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStopResponse
import com.android.security.samples.playintegrityapi.feature.game.data.repository.GameRepository
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SubmitGameScoreUseCaseTest {

    private lateinit var gameRepository: GameRepository
    private lateinit var integrityRepository: IntegrityRepository
    private lateinit var moshi: Moshi
    private lateinit var jsonAdapter: JsonAdapter<GameStopRequest>
    private lateinit var useCase: SubmitGameScoreUseCase

    private val mockRequest = GameStopRequest(
        sessionId = "session-123",
        clientStartTime = 1000L,
        actualTime = 5.0,
        intervalTokens = emptyList()
    )

    private val mockTokenString = "mock.integrity.token.string"
    private lateinit var mockToken: StandardIntegrityToken

    @Before
    fun setup() {
        gameRepository = mock()
        integrityRepository = mock()
        moshi = mock()
        jsonAdapter = mock()
        mockToken = mock()

        whenever(moshi.adapter(GameStopRequest::class.java)).thenReturn(jsonAdapter)
        whenever(jsonAdapter.toJson(any())).thenReturn("{\"fake\":\"payload\"}")

        whenever(mockToken.token()).thenReturn(mockTokenString)

        useCase = SubmitGameScoreUseCase(gameRepository, integrityRepository, moshi)
    }

    @Test
    fun `invoke returns Success when token generation and server submission succeed`() = runTest {
        val expectedResponse = GameStopResponse(status = "OK", message = "Score saved")

        whenever(integrityRepository.requestIntegrityToken(any()))
            .thenReturn(Result.success(mockToken))

        whenever(gameRepository.stopSession(eq(mockTokenString), eq(mockRequest)))
            .thenReturn(Response.success(expectedResponse))

        val result = useCase(mockRequest)

        assertTrue(result is GameResult.Success)
        assertEquals(expectedResponse, (result as GameResult.Success).data)
    }

    @Test
    fun `invoke returns IntegrityError when local token generation fails`() = runTest {
        whenever(integrityRepository.requestIntegrityToken(any()))
            .thenReturn(Result.failure(Exception("Integrity generation failed")))

        val result = useCase(mockRequest)

        assertTrue(result is GameResult.Failure.IntegrityError)
        assertEquals(
            "Failed to sign final payload",
            (result as GameResult.Failure.IntegrityError).message
        )

        verifyNoInteractions(gameRepository)
    }

    @Test
    fun `invoke returns IntegrityError when server rejects the payload with HTTP error`() =
        runTest {
            val mediaType = MediaType.parse("text/plain")
            val errorResponseBody = ResponseBody.create(mediaType, "Cheat detected")

            whenever(integrityRepository.requestIntegrityToken(any()))
                .thenReturn(Result.success(mockToken))

            whenever(gameRepository.stopSession(eq(mockTokenString), eq(mockRequest)))
                .thenReturn(Response.error(403, errorResponseBody))

            val result = useCase(mockRequest)

            assertTrue(result is GameResult.Failure.IntegrityError)
            val errorMessage = (result as GameResult.Failure.IntegrityError).message
            assertTrue(errorMessage.contains("Server rejected score"))
        }

    @Test
    fun `invoke returns NetworkError on network exceptions`() = runTest {
        whenever(integrityRepository.requestIntegrityToken(any()))
            .thenReturn(Result.success(mockToken))

        whenever(gameRepository.stopSession(any(), any()))
            .thenThrow(RuntimeException("Connection timeout"))

        val result = useCase(mockRequest)

        assertTrue(result is GameResult.Failure.NetworkError)
        assertEquals("Failed to submit score", (result as GameResult.Failure.NetworkError).message)
    }

    @Test(expected = CancellationException::class)
    fun `invoke properly propagates CancellationException`() = runTest {
        whenever(integrityRepository.requestIntegrityToken(any()))
            .thenReturn(Result.success(mockToken))

        whenever(gameRepository.stopSession(any(), any()))
            .thenThrow(CancellationException("Job cancelled"))

        useCase(mockRequest)
    }
}