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

            tokenProvider = standardIntegrityManager.prepareIntegrityToken(request).await()
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

            provider.request(request).await()
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
}