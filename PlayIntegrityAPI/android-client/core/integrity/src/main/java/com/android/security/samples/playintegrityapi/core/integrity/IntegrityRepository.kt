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

import android.app.Activity
import com.android.security.samples.playintegrityapi.core.integrity.di.GoogleCloudProjectNumber
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityDialogRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityDialogRequest.StandardIntegrityResponse
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityDialogRequest.StandardIntegrityResponse.ExceptionDetails
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityDialogRequest.StandardIntegrityResponse.TokenResponse
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.play.core.integrity.model.IntegrityErrorCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Reusable repository for fetching Play Integrity tokens and handling remediation.
 */
interface IntegrityRepository {
    /**
     * Warms up the Play Integrity token provider.
     * Call this early (e.g., on app launch) to reduce latency when a token is actually needed.
     */
    suspend fun warmUp(): Result<Unit>

    /**
     * Requests a standard integrity token bound to the provided request hash.
     * Returns the full [StandardIntegrityToken] so it can be used for dialog remediation if needed.
     */
    suspend fun requestIntegrityToken(requestHash: String): Result<StandardIntegrityToken>

    /**
     * Shows a remediation dialog triggered by a server-directed Play Integrity Token.
     *
     * @return A [Result] containing an `Int` that represents the
     *      [IntegrityDialogResponseCode](https://developer.android.com/google/play/integrity/reference/com/google/android/play/core/integrity/model/IntegrityDialogResponseCode)
     *      indicating the user's action.
     */
    suspend fun showDialog(
        activity: Activity, dialogTypeCode: Int, token: StandardIntegrityToken
    ): Result<Int>

    /**
     * Shows a remediation dialog triggered by a client-side StandardIntegrityException.
     *
     * @return A [Result] containing an `Int` that represents the
     *       [IntegrityDialogResponseCode](https://developer.android.com/google/play/integrity/reference/com/google/android/play/core/integrity/model/IntegrityDialogResponseCode)
     *       indicating the user's action.
     */
    suspend fun showDialog(
        activity: Activity, dialogTypeCode: Int, exception: StandardIntegrityException
    ): Result<Int>
}

class IntegrityRepositoryImpl @Inject constructor(
    private val standardIntegrityManager: StandardIntegrityManager,
    @GoogleCloudProjectNumber private val cloudProjectNumber: Long
) : IntegrityRepository {
    private var tokenProvider: StandardIntegrityTokenProvider? = null

    /**
     * Synchronizes the initialization and retrieval of the [tokenProvider].
     * Ensures that if [warmUp] is currently executing, any concurrent calls
     * to [requestIntegrityToken] will suspend and wait for the warmup to
     * complete before proceeding.
     */
    private val mutex = Mutex()

    override suspend fun warmUp(): Result<Unit> = mutex.withLock {
        runCatching {
            val request = PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
                .build()

            retryWithExponentialBackoff {
                tokenProvider = standardIntegrityManager.prepareIntegrityToken(request).await()
            }
        }
    }

    override suspend fun requestIntegrityToken(requestHash: String): Result<StandardIntegrityToken> {
        var provider = mutex.withLock { tokenProvider }
        if (provider == null) {
            warmUp().onFailure { return Result.failure(it) }
            provider = mutex.withLock { tokenProvider } ?: return Result.failure(
                IllegalStateException("Failed to initialize token provider")
            )
        }

        return runCatching {
            val request = StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()

            retryWithExponentialBackoff {
                provider.request(request).await()
            }
        }
    }

    override suspend fun showDialog(
        activity: Activity, dialogTypeCode: Int, token: StandardIntegrityToken
    ): Result<Int> = executeShowDialog(activity, dialogTypeCode, TokenResponse(token))

    override suspend fun showDialog(
        activity: Activity, dialogTypeCode: Int, exception: StandardIntegrityException
    ): Result<Int> = executeShowDialog(activity, dialogTypeCode, ExceptionDetails(exception))

    private suspend fun executeShowDialog(
        activity: Activity, dialogTypeCode: Int, response: StandardIntegrityResponse
    ): Result<Int> = runCatching {
        val dialogRequest = StandardIntegrityDialogRequest.builder()
            .setActivity(activity)
            .setTypeCode(dialogTypeCode)
            .setStandardIntegrityResponse(response)
            .build()

        standardIntegrityManager.showDialog(dialogRequest).await()
    }

    /**
     * Executes a given block of code with exponential backoff if a [StandardIntegrityException]
     * with [IntegrityErrorCode.CLIENT_TRANSIENT_ERROR] is thrown.
     *
     * @param maxAttempts The maximum number of attempts to make. If set to 3, the block will be
     *                    executed up to 3 times (1 initial attempt + 2 retries).
     * @param initialDelay The delay in milliseconds before the first retry attempt.
     * @param maxDelay The maximum delay in milliseconds allowed between retries. This caps the
     *                 delay from growing too large.
     * @param factor The multiplier applied to the current delay for each subsequent retry.
     *               For example, with a factor of 2.0, a 1000ms delay becomes 2000ms, then 4000ms.
     * @param block The suspendable action to execute and potentially retry.
     * @return The result of the [block] if successful.
     */
    private suspend inline fun <T> retryWithExponentialBackoff(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        crossinline block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxAttempts - 1) {
            try {
                return block()
            } catch (e: StandardIntegrityException) {
                if (e.errorCode != IntegrityErrorCode.CLIENT_TRANSIENT_ERROR) {
                    throw e
                }
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block()
    }
}