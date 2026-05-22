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