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

package com.android.security.samples.playintegrityapi.feature.game.data.remote

import com.android.security.samples.playintegrityapi.core.network.NetworkConstants
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class SecurityChecklistDto(
    val isSecure: Boolean,
    val screenCaptureSafe: Boolean,
    val accessibilitySafe: Boolean,
    val playProtectSafe: Boolean
)

@JsonClass(generateAdapter = true)
data class GameChallengeResponse(
    val status: String,
    val challenge: String
)

@JsonClass(generateAdapter = true)
data class GameInitiateRequest(
    val challenge: String
)

@JsonClass(generateAdapter = true)
data class GameInitiateResponse(
    val status: String,
    val sessionId: String,
    val targetTime: Double,
    val intervals: List<Double>,
    val checklist: SecurityChecklistDto
)

@JsonClass(generateAdapter = true)
data class GameStatusResponse(
    val status: String,
    val checklist: SecurityChecklistDto
)

@JsonClass(generateAdapter = true)
data class IntervalTokenDto(
    val interval: Double,
    val token: String
)

@JsonClass(generateAdapter = true)
data class GameStopRequest(
    val actualTime: Double,
    val clientStartTime: Long,
    val intervalTokens: List<IntervalTokenDto>,
    val sessionId: String
)

@JsonClass(generateAdapter = true)
data class GameStopResponse(
    val status: String,
    val message: String,
    @Json(name = "error_code")
    val errorCode: String? = null
)

interface GameApiService {
    @POST("/api/v1/game/challenge")
    suspend fun getChallenge(): Response<GameChallengeResponse>

    @POST("/api/v1/game/initiate")
    suspend fun initiateSession(
        @Header(NetworkConstants.Header.PLAY_INTEGRITY_TOKEN) integrityToken: String,
        @Body request: GameInitiateRequest
    ): Response<GameInitiateResponse>

    @POST("/api/v1/game/status")
    suspend fun getStatus(
        @Header(NetworkConstants.Header.PLAY_INTEGRITY_TOKEN) integrityToken: String
    ): Response<GameStatusResponse>

    @POST("/api/v1/game/stop")
    suspend fun stopSession(
        @Header(NetworkConstants.Header.PLAY_INTEGRITY_TOKEN) integrityToken: String,
        @Body request: GameStopRequest
    ): Response<GameStopResponse>
}