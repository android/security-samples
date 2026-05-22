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

const crypto = require('crypto');
const stringify = require('fast-json-stable-stringify');

/**
 * Utility service for cryptographic operations.
 */
class CryptoService {
    /**
     * Computes an SHA-256 hash of the JSON payload to verify Content Binding.
     * @param {Object} payload
     * @returns {string} Base64 URL-encoded SHA-256 hash
     */
    computePayloadHash(payload) {
        const payloadString = stringify(payload);
        return crypto
            .createHash('sha256')
            .update(payloadString)
            .digest('base64url');
    }
}

module.exports = new CryptoService();