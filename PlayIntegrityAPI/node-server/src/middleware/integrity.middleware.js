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