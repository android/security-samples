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

package com.android.security.samples.playintegrityapi.feature.bank.domain

import android.util.Log
import com.android.security.samples.playintegrityapi.core.integrity.IntegrityRepository
import com.android.security.samples.playintegrityapi.core.integrity.Utils.generateSha256Hash
import com.android.security.samples.playintegrityapi.feature.bank.data.remote.TransferErrorResponse
import com.android.security.samples.playintegrityapi.feature.bank.data.remote.TransferRequest
import com.android.security.samples.playintegrityapi.feature.bank.data.repository.BankRepository
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

sealed interface TransferResult {
    data class Success(val transactionId: String, val message: String) : TransferResult

    sealed interface Failure : TransferResult {
        data class NetworkError(val message: String) : Failure

        sealed interface IntegrityError : Failure {
            val message: String

            data class Server(
                override val message: String,
                val remediationDialogTypeCode: Int?,
                val token: StandardIntegrityToken
            ) : IntegrityError

            data class Client(
                override val message: String,
                val exception: StandardIntegrityException?
            ) : IntegrityError
        }
    }
}

class SubmitSecureTransferUseCase @Inject constructor(
    private val bankRepository: BankRepository,
    private val integrityRepository: IntegrityRepository,
    private val moshi: Moshi
) {
    private val requestAdapter = moshi.adapter(TransferRequest::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    private companion object {
        const val TAG = "SecureTransferUseCase"
    }

    suspend operator fun invoke(accountNumber: String, amount: BigDecimal): TransferResult {
        Log.d(TAG, "--- Initiating Secure Transfer ---")
        Log.d(TAG, "Amount: $amount, Account: ${accountNumber.takeLast(4).padStart(accountNumber.length, '*')}")

        val request = createTransferRequest(accountNumber, amount)
        val requestHash = generateContentBindingHash(request)
            ?: return TransferResult.Failure.NetworkError("Internal data processing error")

        val tokenResult = fetchIntegrityToken(requestHash)
        if (tokenResult.isFailure) {
            return handleTokenFailure(tokenResult.exceptionOrNull())
        }

        val integrityToken = tokenResult.getOrThrow()
        return executeAndProcessTransfer(request, integrityToken)
    }

    private fun createTransferRequest(accountNumber: String, amount: BigDecimal): TransferRequest {
        // NOTE: While Play Integrity API Standard Mode offers automatic replay protection,
        // it only prevents a single token from being verified excessively. See https://developer.android.com/google/play/integrity/standard#replay-protection
        // To strictly prevent duplicate requests (exactly-once execution), an app must implement
        // its own idempotency mechanism using a unique key.
        return TransferRequest(
            amount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            accountNumber = accountNumber,
            idempotencyKey = UUID.randomUUID().toString()
        )
    }

    // Request hash generation:
    // When the user initiates the transfer, the client serializes the transaction
    // details into a JSON string and computes its SHA-256 hash. The resulting
    // Base64-encoded string is passed as the requestHash parameter into the token provider.
    private fun generateContentBindingHash(request: TransferRequest): String? {
        return try {
            val rawJson = requestAdapter.toJson(request)
            val map = mapAdapter.fromJson(rawJson) ?: return null
            val canonicalJson = mapAdapter.toJson(map.toSortedMap())
            val hash = generateSha256Hash(canonicalJson)
            Log.d(TAG, "1. Content-binding hash generated: $hash")
            hash
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize transfer request", e)
            null
        }
    }

    private suspend fun fetchIntegrityToken(requestHash: String): Result<StandardIntegrityToken> {
        Log.d(TAG, "2. Requesting Standard Integrity Token from Play Core...")
        return integrityRepository.requestIntegrityToken(requestHash).onSuccess { token ->
            Log.d(TAG, "-> Token successfully retrieved. Length: ${token.token().length}")
        }
    }

    private fun handleTokenFailure(throwable: Throwable?): TransferResult.Failure.IntegrityError.Client {
        val exception = throwable as? StandardIntegrityException
        Log.e(
            TAG,
            "-> Play Integrity Token generation failed locally! Code: ${exception?.errorCode}",
            exception
        )

        return TransferResult.Failure.IntegrityError.Client(
            message = "Device integrity check failed locally.",
            exception = exception
        )
    }

    // Network execution:
    // Once the Play Integrity token is generated, this method invokes the BankRepository,
    // placing the raw JSON transaction in the HTTP body and injecting the token
    // into the HTTP header for submission to the server.
    private suspend fun executeAndProcessTransfer(
        request: TransferRequest,
        integrityToken: StandardIntegrityToken
    ): TransferResult {
        Log.d(TAG, "Submitting transaction to Bank Server...")

        val response = try {
            bankRepository.submitTransfer(integrityToken.token(), request)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            Log.e(TAG, "!!! Network exception during transfer !!!", e)
            return TransferResult.Failure.NetworkError(e.localizedMessage ?: "Network connection failed")
        }

        if (response.isSuccessful && response.body() != null) {
            val transactionId = response.body()!!.transactionId ?: "UNKNOWN"
            Log.d(TAG, "-> Server SUCCESS! HTTP ${response.code()} | Transaction ID: $transactionId")

            return TransferResult.Success(
                transactionId = transactionId,
                message = response.body()!!.message
            )
        }

        return handleServerFailure(
            responseCode = response.code(),
            errorJson = response.errorBody()?.string(),
            integrityToken = integrityToken
        )
    }

    // Handling remediation (Step 1: Parsing):
    // If the backend decides the device or app does not meet the required security
    // policy, it returns a 403 Forbidden with a structured JSON payload.
    // This method parses that 403 error and extracts the remediationCode.
    private fun handleServerFailure(
        responseCode: Int,
        errorJson: String?,
        integrityToken: StandardIntegrityToken
    ): TransferResult.Failure.IntegrityError.Server {
        Log.e(TAG, "-> Server REJECTED request! HTTP $responseCode")
        Log.e(TAG, "-> Raw Error Body: $errorJson")

        var parsedRemediationCode: Int? = null
        var parsedErrorMessage = "Server rejected the transaction."

        if (!errorJson.isNullOrBlank()) {
            try {
                val errorAdapter = moshi.adapter(TransferErrorResponse::class.java)
                val errorResponse = errorAdapter.fromJson(errorJson)
                parsedRemediationCode = errorResponse?.remediationCode
                parsedErrorMessage = errorResponse?.message ?: parsedErrorMessage

                Log.d(TAG, "-> Parsed Remediation Code: $parsedRemediationCode")
            } catch (e: Exception) {
                Log.e(TAG, "-> Failed to parse error response JSON", e)
            }
        }

        return TransferResult.Failure.IntegrityError.Server(
            message = parsedErrorMessage,
            remediationDialogTypeCode = parsedRemediationCode,
            token = integrityToken
        )
    }
}