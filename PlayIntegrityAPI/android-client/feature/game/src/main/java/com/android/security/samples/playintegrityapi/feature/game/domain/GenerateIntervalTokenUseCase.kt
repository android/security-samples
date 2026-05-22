package com.android.security.samples.playintegrityapi.feature.game.domain

import android.util.Log
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.Utils.generateSha256Hash
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import javax.inject.Inject

/**
 * Handles the TOCTOU cryptography by generating mathematically bound background tokens.
 */
class GenerateIntervalTokenUseCase @Inject constructor(
    private val integrityRepository: IntegrityRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        clientStartTime: Long,
        interval: Double
    ): Result<StandardIntegrityToken> {
        val hashSource = "$sessionId$clientStartTime$interval"
        val requestHash = generateSha256Hash(hashSource)

        Log.d("GenerateIntervalToken", "Polling interval $interval with hash: $requestHash")
        return integrityRepository.requestIntegrityToken(requestHash)
    }
}
