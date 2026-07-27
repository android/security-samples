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

const { StatusCodes } = require('http-status-codes');
const cryptoService = require('../../services/crypto.service');
const streamingPolicy = require('./streaming.policy');
const manifestService = require('./manifest.service');
const { STREAM_QUALITIES } = require('./streaming.constants');

class StreamingController {

    constructor() {
        this.getManifest = this.getManifest.bind(this);
    }

    async getManifest(req, res, next) {
        try {
            // Token Decoding & Replay Protection:
            // The token is extracted and decoded via the `integrity.middleware`.
            // Because we use Standard requests, Google's server automatically detects
            // and rejects replayed tokens.
            const integrityPayload = res.locals.integrityPayload;
            const contentId = req.params.contentId;

            // Determine the maximum allowed resolution based on PlayIntegrityAPI result
            const maxQuality = this.#determineMaxQuality(integrityPayload, contentId);

            // Fetch and send the dynamically filtered manifest
            const customManifestXml = await manifestService.getFilteredManifest(maxQuality);

            res.set('Content-Type', 'application/dash+xml');
            res.status(StatusCodes.OK).send(customManifestXml);

        } catch (error) {
            next(error);
        }
    }

    /**
     * Determines the maximum video resolution tier based on the Play Integrity payload.
     * Falls back to the RESTRICTED tier if any validation fails to ensure graceful degradation.
     *
     * @param {Object} integrityPayload - The decoded Play Integrity API token payload
     * @param {string} contentId - The ID of the requested media
     * @returns {number} The maximum allowed height
     */
    #determineMaxQuality(integrityPayload, contentId) {
        if (!integrityPayload) {
            console.warn(`[Streaming API] Missing or invalid token. Falling back to ${STREAM_QUALITIES.RESTRICTED}p.`);
            return STREAM_QUALITIES.RESTRICTED;
        }

        if (!this.#isContentBindingValid(integrityPayload, contentId)) {
            console.warn(`[Streaming API] Token hash mismatch. Falling back to ${STREAM_QUALITIES.RESTRICTED}p.`);
            return STREAM_QUALITIES.RESTRICTED;
        }

        return streamingPolicy.evaluateStreamQuality(integrityPayload);
    }

    /**
     * Verifies that the token's request hash matches the requested content and action.
     *
     * @param {Object} integrityPayload - The decoded Play Integrity API token payload
     * @param {string} contentId - The ID of the requested media
     * @returns {boolean} True if the hashes match perfectly
     */
    #isContentBindingValid(integrityPayload, contentId) {
        const payload = {
            action: 'fetch_manifest',
            contentId: contentId
        };

        const expectedHash = cryptoService.computePayloadHash(payload);
        const tokenHash = integrityPayload.requestDetails?.requestHash;

        return expectedHash === tokenHash;
    }
}

module.exports = new StreamingController();
