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
    val accountNumber: String
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