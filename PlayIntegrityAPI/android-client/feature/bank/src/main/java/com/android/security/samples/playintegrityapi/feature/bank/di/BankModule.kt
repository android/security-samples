package com.android.security.samples.playintegrityapi.feature.bank.di

import com.android.security.samples.playintegrityapi.feature.bank.data.remote.BankApiService
import com.android.security.samples.playintegrityapi.feature.bank.data.repository.BankRepository
import com.android.security.samples.playintegrityapi.feature.bank.data.repository.BankRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BankModule {

    @Binds
    @Singleton
    abstract fun bindBankRepository(
        bankRepositoryImpl: BankRepositoryImpl
    ): BankRepository

    companion object {

        @Provides
        @Singleton
        fun provideBankApiService(retrofit: Retrofit): BankApiService {
            return retrofit.create(BankApiService::class.java)
        }
    }
}