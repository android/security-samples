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
