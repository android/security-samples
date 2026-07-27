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

package com.android.security.samples.playintegrityapi.feature.bank.data.remote

import com.android.security.samples.playintegrityapi.core.network.NetworkConstants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TransferRequest(
    val amount: String,
    val accountNumber: String,
    val idempotencyKey: String
)

@JsonClass(generateAdapter = true)
data class TransferResponse(
    val status: String,
    val transactionId: String?,
    val message: String
)

@JsonClass(generateAdapter = true)
data class TransferErrorResponse(
    val status: String,

    @Json(name = "error_code")
    val errorCode: String,

    val message: String,

    @Json(name = "remediation_code")
    val remediationCode: Int?,

    @Json(name = "remediation_action")
    val remediationAction: String?
)

interface BankApiService {
    @POST("/api/v1/bank/transfer")
    suspend fun executeTransfer(
        @Header(NetworkConstants.Header.PLAY_INTEGRITY_TOKEN) integrityToken: String,
        @Body request: TransferRequest
    ): Response<TransferResponse>
}