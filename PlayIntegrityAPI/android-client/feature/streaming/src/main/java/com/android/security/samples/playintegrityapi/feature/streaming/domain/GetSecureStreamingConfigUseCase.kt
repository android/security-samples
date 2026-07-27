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
    private val integrityRepository: IntegrityRepository
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
     * Request Hash Generation (Content Binding):
     * The streaming payload is simply the requested video context. The client manually
     * constructs a tight JSON string and generates a SHA-256 hash of this string to produce
     * the requestHash.
     *
     * In a production environment, you should strengthen this binding further. Instead
     * of just hashing the action and content ID, consider including a non sensitive
     * user-specific identifier within the hashed data (e.g. a sessionId or userId).
     *
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
        if (forceWarmup) {
            integrityRepository.warmUp()
        }

        Log.d(TAG, "Requesting Standard Integrity Token...")
        val tokenResult = integrityRepository.requestIntegrityToken(requestHash = requestHash)

        if (tokenResult.isSuccess) {
            Log.d(TAG, "-> Token successfully retrieved.")
            return tokenResult.getOrThrow().token()
        }

        // -------------------------------------------------------------------------
        // DESIGN CHOICE: UX vs. Security Trade-off
        // Returning null here instead of throwing an exception is intentional.
        // This "fail-open" behavior is a product decision to ensure that users
        // are always presented with playback, even if the Play Integrity token
        // generation fails locally.
        //
        // BACKEND ENFORCEMENT: By returning null, the backend server detects the
        // missing token and falls back to serving a restricted, lowest-tier streaming
        // configuration to maintain security.
        //
        // STRICT SECURITY REQUIREMENT: If your app demands strict security (e.g.,
        // high-value premium content, financial transactions), implement a "fail-closed"
        // strategy here by throwing an exception or returning a StreamingResult.Failure.
        // -------------------------------------------------------------------------
        Log.w(
            TAG,
            "-> Token generation failed locally. Proceeding without token to trigger lowest tier fallback."
        )
        return null
    }
}