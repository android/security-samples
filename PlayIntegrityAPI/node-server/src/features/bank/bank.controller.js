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

const {StatusCodes} = require('http-status-codes');
const cryptoService = require('../../services/crypto.service');
const bankPolicy = require('./bank.policy');

// NOTE: For simplicity in this sample app, processed idempotency keys are stored in an in-memory Set.
// In a real-world production environment:
// 1. Use a distributed, highly available data store (such as Redis) rather than server memory.
// 2. Set an explicit Time-To-Live (TTL) expiration on keys (e.g., 24 to 48 hours) to prevent unbounded memory growth.
// 3. Ensure the check-and-set operation is executed atomically (e.g., using Redis SETNX) to avoid race conditions.
const processedTransactions = new Set();

/**
 * Controller for handling Bank micro-app endpoints.
 */
class BankController {

    /**
     * Handles the POST request for a bank transfer.
     *
     * @param {Object} req - Express request object containing the payload and headers.
     * @param {Object} res - Express response object used to send verdicts or success data.
     * @param {Function} next - Express middleware function for error delegation.
     */
    async handleTransfer(req, res, next) {
        // Access the payload attached by the integrity middleware
        const tokenPayload = res.locals.integrityPayload;
        if (!tokenPayload) {
            return res.status(StatusCodes.UNAUTHORIZED).json({
                status: "ERROR",
                error_code: "UNAUTHORIZED",
                message: "A valid Play Integrity token is required for the transaction."
            });
        }

        // Verify Content Binding
        const tokenRequestHash = tokenPayload.requestDetails?.requestHash;
        const payload = req.body;
        try {
            const serverRequestHash = cryptoService.computePayloadHash(payload);
            if (serverRequestHash !== tokenRequestHash) {
                return res.status(StatusCodes.FORBIDDEN).json({
                    status: "ERROR", error_code: "REQUEST_TAMPERED", message: "The request payload has been altered."
                });
            }
        } catch (error) {
            next(error);
        }

        // Evaluate Play Integrity token payload against the Bank transfer policy
        const isPolicyMet = bankPolicy.evaluateTransferPolicy(tokenPayload);
        if (!isPolicyMet) {
            return res.status(StatusCodes.FORBIDDEN).json({
                status: "ERROR",
                error_code: "INTEGRITY_REJECTED",
                message: "Device does not meet the required security standards.",
                remediation_code: 4,
                remediation_action: "GET_INTEGRITY"
            });
        }

        // Validate Idempotency Key for duplicate transaction prevention
        // PRODUCTION NOTE: While Play Integrity API Standard Mode provides automatic
        // replay protection, it only prevents a token from being decoded/replayed
        // excessively. See https://developer.android.com/google/play/integrity/standard#replay-protection.
        // For strict, exactly-once operations like financial transfers, apps cannot rely solely on PIA's automatic replay
        // protection. You must implement your own idempotency check using a unique key.
        const idempotencyKey = payload.idempotencyKey;
        if (!idempotencyKey) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                status: "ERROR",
                error_code: "MISSING_IDEMPOTENCY_KEY",
                message: "An idempotency key is required to process the transfer."
            });
        }
        if (processedTransactions.has(idempotencyKey)) {
            return res.status(StatusCodes.CONFLICT).json({
                status: "ERROR",
                error_code: "DUPLICATE_TRANSACTION",
                message: "A transaction with this idempotency key has already been processed."
            });
        }

        // PRODUCTION NOTE: Register the idempotency key in Redis/DB *after* all
        // validations pass, ideally alongside the actual database transaction commit.
        processedTransactions.add(idempotencyKey);
        return res.status(StatusCodes.OK).json({
            status: "SUCCESS",
            transactionId: `TXN-${Math.floor(Math.random() * 1000000000)}`,
            message: "Transfer completed successfully."
        });
    }
}

module.exports = new BankController();