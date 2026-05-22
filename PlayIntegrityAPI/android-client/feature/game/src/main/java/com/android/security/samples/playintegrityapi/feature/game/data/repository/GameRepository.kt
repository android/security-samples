package com.android.security.samples.playintegrityapi.feature.game.data.repository

import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameApiService
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameInitiateResponse
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStatusResponse
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStopRequest
import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameStopResponse
import retrofit2.Response
import javax.inject.Inject

interface GameRepository {
    suspend fun initiateSession(token: String): Response<GameInitiateResponse>
    suspend fun getStatus(token: String): Response<GameStatusResponse>
    suspend fun stopSession(token: String, request: GameStopRequest): Response<GameStopResponse>
}

class GameRepositoryImpl @Inject constructor(
    private val gameApiService: GameApiService
) : GameRepository {
    override suspend fun initiateSession(token: String): Response<GameInitiateResponse> =
        gameApiService.initiateSession(token)

    override suspend fun getStatus(token: String): Response<GameStatusResponse> =
        gameApiService.getStatus(token)

    override suspend fun stopSession(token: String, request: GameStopRequest): Response<GameStopResponse> =
        gameApiService.stopSession(token, request)
}