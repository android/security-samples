package com.android.security.samples.playintegrityapi.feature.streaming.domain

import android.util.Log
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.Utils.generateSha256Hash
import com.android.security.samples.playintegrityapi.core.integrity.di.GoogleCloudProjectNumber
import com.android.security.samples.playintegrityapi.core.network.BuildConfig
import javax.inject.Inject

data class SecureStreamingConfig(
    val manifestUrl: String,
    val playIntegrityToken: String?
)

sealed interface StreamingResult {
    data class Success(val config: SecureStreamingConfig) : StreamingResult
    data class Failure(val message: String, val exception: Exception? = null) : StreamingResult
}

class GetSecureStreamingConfigUseCase @Inject constructor(
    private val integrityRepository: IntegrityRepository,
    @GoogleCloudProjectNumber private val gcpProjectNumber: Long
) {
    private companion object {
        const val TAG = "StreamingUseCase"
        const val CONTENT_ID = "sample_video_01"
        const val LOCAL_MANIFEST_ENDPOINT =
            "${BuildConfig.BASE_URL}api/v1/streaming/$CONTENT_ID/manifest.mpd"
    }

    suspend operator fun invoke(forceWarmup: Boolean): StreamingResult {
        Log.d(TAG, "--- Initiating Secure Stream Request ---")

        return try {
            val requestHash = generateContentBindingHash()
            val integrityToken = fetchIntegrityToken(requestHash, forceWarmup)

            StreamingResult.Success(
                SecureStreamingConfig(LOCAL_MANIFEST_ENDPOINT, integrityToken)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare streaming config", e)
            StreamingResult.Failure(e.localizedMessage ?: "Unknown error occurred", e)
        }
    }

    /**
     * Constructs the JSON payload and generates a SHA-256 hash.
     * WARNING: Cryptographic hashes are extremely sensitive to whitespace.
     * This string must precisely match how the server stringifies its payload.
     */
    private fun generateContentBindingHash(): String {
        val jsonPayload = """{"action":"fetch_manifest","contentId":"$CONTENT_ID"}"""
        val requestHash = generateSha256Hash(jsonPayload)

        Log.d(TAG, "Content-binding hash generated for $CONTENT_ID: $requestHash")
        return requestHash
    }

    /**
     * Warms up the integrity provider and requests a token bound to the provided hash.
     * Implements graceful degradation by returning null if the local token request fails.
     */
    private suspend fun fetchIntegrityToken(requestHash: String, forceWarmup: Boolean): String? {
        if(forceWarmup){
            integrityRepository.warmUp()
        }

        Log.d(TAG, "Requesting Standard Integrity Token...")
        val tokenResult = integrityRepository.requestIntegrityToken(requestHash = requestHash)

        return if (tokenResult.isSuccess) {
            Log.d(TAG, "-> Token successfully retrieved.")
            tokenResult.getOrThrow().token()
        } else {
            Log.w(
                TAG,
                "-> Token generation failed locally. Proceeding without token to trigger lowest tier fallback."
            )
            null
        }
    }
}