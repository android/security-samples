package com.android.security.samples.playintegrityapi.feature.bank.data.repository

import com.android.security.samples.playintegrityapi.feature.bank.data.remote.BankApiService
import com.android.security.samples.playintegrityapi.feature.bank.data.remote.TransferRequest
import com.android.security.samples.playintegrityapi.feature.bank.data.remote.TransferResponse
import retrofit2.Response
import javax.inject.Inject

interface BankRepository {
    suspend fun submitTransfer(token: String, request: TransferRequest): Response<TransferResponse>
}

class BankRepositoryImpl @Inject constructor(
    private val bankApiService: BankApiService
) : BankRepository {
    override suspend fun submitTransfer(
        token: String,
        request: TransferRequest
    ): Response<TransferResponse> {
        return bankApiService.executeTransfer(token, request)
    }
}