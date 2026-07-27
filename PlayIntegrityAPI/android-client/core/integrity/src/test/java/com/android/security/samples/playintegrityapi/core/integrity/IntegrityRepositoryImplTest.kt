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

package com.android.security.samples.playintegrityapi.core.integrity

import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.model.IntegrityErrorCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [IntegrityRepositoryImpl].
 * 
 * Verifies the repository's interaction with [StandardIntegrityManager], ensuring proper
 * token fetching, error propagation, and exponential backoff handling for transient errors.
 */
class IntegrityRepositoryImplTest {

    private lateinit var standardIntegrityManager: StandardIntegrityManager
    private lateinit var repository: IntegrityRepositoryImpl
    private val cloudProjectNumber: Long = 123456789L

    @Before
    fun setup() {
        standardIntegrityManager = mock()
        repository = IntegrityRepositoryImpl(standardIntegrityManager, cloudProjectNumber)
    }

    @Test
    fun `warmUp retries on CLIENT_TRANSIENT_ERROR and eventually succeeds`() = runTest {
        val mockProvider = mock<StandardIntegrityTokenProvider>()
        val exception = mock<StandardIntegrityException>()
        whenever(exception.errorCode).thenReturn(IntegrityErrorCode.CLIENT_TRANSIENT_ERROR)
        whenever(standardIntegrityManager.prepareIntegrityToken(any()))
            .thenReturn(Tasks.forException(exception))
            .thenReturn(Tasks.forException(exception))
            .thenReturn(Tasks.forResult(mockProvider))

        val result = repository.warmUp()

        assertTrue(result.isSuccess)
        verify(standardIntegrityManager, times(3)).prepareIntegrityToken(any())
    }

    @Test
    fun `warmUp fails immediately on non-transient error`() = runTest {
        val exception = mock<StandardIntegrityException>()
        whenever(exception.errorCode).thenReturn(IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE)
        whenever(standardIntegrityManager.prepareIntegrityToken(any()))
            .thenReturn(Tasks.forException(exception))

        val result = repository.warmUp()

        assertTrue(result.isFailure)
        verify(standardIntegrityManager, times(1)).prepareIntegrityToken(any())
    }

    @Test
    fun `warmUp gives up and fails after maxAttempts of transient errors`() = runTest {
        val exception = mock<StandardIntegrityException>()
        whenever(exception.errorCode).thenReturn(IntegrityErrorCode.CLIENT_TRANSIENT_ERROR)
        whenever(standardIntegrityManager.prepareIntegrityToken(any()))
            .thenReturn(Tasks.forException(exception))

        val result = repository.warmUp()

        assertTrue(result.isFailure)
        verify(standardIntegrityManager, times(3)).prepareIntegrityToken(any())
    }
}
