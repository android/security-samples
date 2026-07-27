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

import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.Utils.generateSha256Hash
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStatusResponse
import com.android.security.samples.playintegrityapi.feature.game.data.repository.GameRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class GetGameStatusUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val integrityRepository: IntegrityRepository
) {
    suspend operator fun invoke(): GameResult<GameStatusResponse> {
        // DEVELOPER NOTE: Using System.currentTimeMillis() as a local nonce is used here
        // because this status check is for UI debugging purposes (i.e. updating UI state to reflect changes
        // in environment signals like Play Protect turned off, or App Access Risk) and is not enforced at all.
        val requestHash = generateSha256Hash("status_check_${System.currentTimeMillis()}")
        val tokenResult = integrityRepository.requestIntegrityToken(requestHash)

        if (tokenResult.isFailure) return GameResult.Failure.IntegrityError("Token failed")

        return try {
            val response = gameRepository.getStatus(tokenResult.getOrThrow().token())
            if (response.isSuccessful && response.body() != null) GameResult.Success(response.body()!!)
            else GameResult.Failure.NetworkError("Failed to fetch status")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            GameResult.Failure.NetworkError("Network error")
        }
    }
}
