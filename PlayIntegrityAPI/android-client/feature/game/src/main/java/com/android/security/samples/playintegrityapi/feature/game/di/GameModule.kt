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

package com.android.security.samples.playintegrityapi.feature.game.di

import com.android.security.samples.playintegrityapi.feature.game.data.remote.GameApiService
import com.android.security.samples.playintegrityapi.feature.game.data.repository.GameRepository
import com.android.security.samples.playintegrityapi.feature.game.data.repository.GameRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GameModule {

    @Binds
    @Singleton
    abstract fun bindGameRepository(
        gameRepositoryImpl: GameRepositoryImpl
    ): GameRepository

    companion object {

        @Provides
        @Singleton
        fun provideGameApiService(retrofit: Retrofit): GameApiService {
            return retrofit.create(GameApiService::class.java)
        }
    }
}