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
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameInitiateResponse
import com.android.security.samples.playintegrityapi.feature.game.data.repository.GameRepository
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException


import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameInitiateRequest

class InitiateGameUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val integrityRepository: IntegrityRepository
) {
    // Token preparation and session initialisation:
    // When the user taps Start Secure Session, the client fetches a server-generated challenge,
    // hashes it, and requests a Play Integrity token bound to this challenge.
    // It then calls POST /api/v1/game/initiate with the token.
    // The server returns a unique sessionId, the game's targetTime, and an array of
    // randomised check-in intervals (e.g. [2.5, 5.12, 8.3]).
    suspend operator fun invoke(): GameResult<GameInitiateResponse> {
        val challengeResponse = gameRepository.getChallenge()
        if (!challengeResponse.isSuccessful || challengeResponse.body() == null) {
            return GameResult.Failure.NetworkError("Failed to fetch server challenge")
        }
        val challenge = challengeResponse.body()!!.challenge
        val jsonPayload = """{"challenge":"$challenge"}"""
        val requestHash = generateSha256Hash(jsonPayload)
        
        integrityRepository.warmUp()
        val tokenResult = integrityRepository.requestIntegrityToken(requestHash)

        if (tokenResult.isFailure) return GameResult.Failure.IntegrityError("Failed to generate local token")

        return try {
            val request = GameInitiateRequest(challenge)
            val response = gameRepository.initiateSession(tokenResult.getOrThrow().token(), request)
            if (response.isSuccessful && response.body() != null) {
                GameResult.Success(response.body()!!)
            } else {
                GameResult.Failure.NetworkError("Server rejected initiation: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            GameResult.Failure.NetworkError(e.message ?: "Network error")
        }
    }
}