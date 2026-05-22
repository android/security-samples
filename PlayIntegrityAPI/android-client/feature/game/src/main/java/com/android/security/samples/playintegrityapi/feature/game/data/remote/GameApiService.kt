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
    val sessionId: String,
    val clientStartTime: Long,
    val actualTime: Double,
    val intervalTokens: List<IntervalTokenDto>
)

@JsonClass(generateAdapter = true)
data class GameStopResponse(
    val status: String,
    val message: String,
    @Json(name = "error_code")
    val errorCode: String? = null
)

interface GameApiService {
    @POST("/api/v1/game/initiate")
    suspend fun initiateSession(
        @Header(NetworkConstants.Header.PLAY_INTEGRITY_TOKEN) integrityToken: String
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