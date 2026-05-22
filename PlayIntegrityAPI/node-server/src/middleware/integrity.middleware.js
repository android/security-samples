const integrityService = require('../services/integrity.service');
const { HEADERS } = require('../config/constants');

/**
 * Extracts and decodes the Play Integrity token. If successful, attaches the
 * parsed verdicts to res.locals. If missing or invalid, attaches 'null',
 * leaving the failure penalty up to the specific route controller.
 */
async function extractIntegrityToken(req, res, next) {
    const integrityToken = req.headers[HEADERS.PLAY_INTEGRITY_TOKEN];
    if (!integrityToken) {
        res.locals.integrityPayload = null;
        return next();
    }

    try {
        res.locals.integrityPayload = await integrityService.decodeToken(integrityToken);
        next();

    } catch (error) {
        console.warn(`[Integrity Middleware] Token decode failed: ${error.message}`);
        res.locals.integrityPayload = null;
        next();
    }
}

module.exports = extractIntegrityToken;