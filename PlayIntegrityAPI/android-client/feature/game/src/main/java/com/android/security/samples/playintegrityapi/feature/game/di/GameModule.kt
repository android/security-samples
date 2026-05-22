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