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