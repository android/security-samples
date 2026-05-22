package com.android.security.samples.playintegrityapi.core.integrity.di

import android.content.Context
import com.android.security.samples.playintegrityapi.core.integrity.BuildConfig
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepositoryImpl
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleCloudProjectNumber

@Module
@InstallIn(SingletonComponent::class)
abstract class IntegrityModule {

    @Binds
    @Singleton
    abstract fun bindIntegrityRepository(
        integrityRepositoryImpl: IntegrityRepositoryImpl
    ): IntegrityRepository

    companion object {
        @Provides
        @Singleton
        fun provideStandardIntegrityManager(
            @ApplicationContext context: Context
        ): StandardIntegrityManager {
            return IntegrityManagerFactory.createStandard(context)
        }

        @Provides
        @GoogleCloudProjectNumber
        fun provideGcpProjectNumber(): Long {
            return BuildConfig.GCP_PROJECT_NUMBER
        }
    }
}