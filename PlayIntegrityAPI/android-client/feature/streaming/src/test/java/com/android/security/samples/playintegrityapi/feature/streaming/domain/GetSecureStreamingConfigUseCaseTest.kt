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

package com.android.security.samples.playintegrityapi.feature.streaming.domain

import android.util.Base64
import android.util.Log
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetSecureStreamingConfigUseCaseTest {

    private lateinit var integrityRepository: IntegrityRepository
    private lateinit var getSecureStreamingConfigUseCase: GetSecureStreamingConfigUseCase

    private lateinit var logMock: MockedStatic<Log>
    private lateinit var base64Mock: MockedStatic<Base64>

    private val testGcpProjectNumber = 1234567890L
    private val expectedManifestUrl = "http://10.0.2.2:3000/api/v1/streaming/sample_video_01/manifest.mpd"

    @Before
    fun setup() {
        integrityRepository = mock()
        getSecureStreamingConfigUseCase = GetSecureStreamingConfigUseCase(integrityRepository, testGcpProjectNumber)

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
    fun `invoke calls warmUp when forceWarmup is true`() = runTest {
        val mockToken = mock<StandardIntegrityToken>()
        whenever(integrityRepository.requestIntegrityToken(anyOrNull()))
            .thenReturn(Result.success(mockToken))

        getSecureStreamingConfigUseCase(forceWarmup = true)

        verify(integrityRepository).warmUp()
    }

    @Test
    fun `invoke does not call warmUp when forceWarmup is false`() = runTest {
        val mockToken = mock<StandardIntegrityToken>()
        whenever(integrityRepository.requestIntegrityToken(anyOrNull()))
            .thenReturn(Result.success(mockToken))

        getSecureStreamingConfigUseCase(forceWarmup = false)

        verify(integrityRepository, never()).warmUp()
    }

    @Test
    fun `invoke returns Success with token when local Play Core generation succeeds`() = runTest {
        val mockToken = mock<StandardIntegrityToken>()
        whenever(mockToken.token()).thenReturn("valid_token_string")
        whenever(integrityRepository.requestIntegrityToken(anyOrNull()))
            .thenReturn(Result.success(mockToken))

        val result = getSecureStreamingConfigUseCase(forceWarmup = false)

        assertTrue(result is StreamingResult.Success)
        val successResult = result as StreamingResult.Success
        assertEquals(expectedManifestUrl, successResult.config.manifestUrl)
        assertEquals("valid_token_string", successResult.config.playIntegrityToken)
    }

    @Test
    fun `invoke returns Success with null token when Play Core fails`() = runTest {
        val exception = mock<StandardIntegrityException>()
        whenever(integrityRepository.requestIntegrityToken(anyOrNull()))
            .thenReturn(Result.failure(exception))

        val result = getSecureStreamingConfigUseCase(forceWarmup = false)

        assertTrue(result is StreamingResult.Success)
        val successResult = result as StreamingResult.Success
        assertEquals(expectedManifestUrl, successResult.config.manifestUrl)
        assertNull(successResult.config.playIntegrityToken)
    }

    @Test
    fun `invoke returns Failure when an unhandled exception is thrown`() = runTest {
        whenever(integrityRepository.warmUp())
            .thenThrow(RuntimeException("Unknown system error"))

        val result = getSecureStreamingConfigUseCase(forceWarmup = true)

        assertTrue(result is StreamingResult.Failure)
        val failureResult = result as StreamingResult.Failure
        assertEquals("Unknown system error", failureResult.message)
    }
}