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