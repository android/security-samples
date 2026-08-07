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
import com.android.security.samples.playintegrityapi.feature.game.data.remote.*
import com.android.security.samples.playintegrityapi.feature.game.data.repository.GameRepository
import com.squareup.moshi.Moshi
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class SubmitGameScoreUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val integrityRepository: IntegrityRepository,
    moshi: Moshi
) {
    private val requestAdapter = moshi.adapter(GameStopRequest::class.java)

    // Stopping the game:
    // When the user stops the timer, the client compiles the final actualTime, the
    // sessionId, the clientStartTime, and the array of all intervalTokens into a JSON
    // payload. It hashes this entire JSON string, requests a final Play Integrity token,
    // and sends the lot to POST /api/v1/game/stop.
    suspend operator fun invoke(request: GameStopRequest): GameResult<GameStopResponse> {
        val jsonPayload = requestAdapter.toJson(request)
        val requestHash = generateSha256Hash(jsonPayload)

        val tokenResult = integrityRepository.requestIntegrityToken(requestHash)
        if (tokenResult.isFailure) return GameResult.Failure.IntegrityError("Failed to sign final payload")

        val token = tokenResult.getOrThrow()

        return try {
            val response = gameRepository.stopSession(token.token(), request)
            if (response.isSuccessful && response.body() != null) {
                GameResult.Success(response.body()!!)
            } else {
                GameResult.Failure.IntegrityError(
                    message = "Server rejected score: ${response.errorBody()?.string()}",
                    token = token
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            GameResult.Failure.NetworkError("Failed to submit score")
        }
    }
}