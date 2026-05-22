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
            const integrityPayload = res.locals.integrityPayload;
            const contentId = req.params.contentId;

            // Determine the maximum allowed resolution based on PlayIntegrityAPI result
            const maxQuality = this.#determineMaxQuality(integrityPayload, contentId);

            // Fetch and send the dynamically filtered manifest
            const customManifestXml = await manifestService.getFilteredManifest(maxQuality);

            res.set('Content-Type', 'application/dash+xml');
            res.status(200).send(customManifestXml);

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
