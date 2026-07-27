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
const crypto = require('crypto');
const gamePolicy = require('./game.policy');
const cryptoService = require('../../services/crypto.service');

// In-memory store for active sessions
const activeSessions = new Map();
// In-memory store for active challenges (challenge -> expiryTimestamp)
const activeChallenges = new Map();

class GameController {

    constructor() {
        this.getChallenge = this.getChallenge.bind(this);
        this.initiate = this.initiate.bind(this);
        this.getStatus = this.getStatus.bind(this);
        this.stop = this.stop.bind(this);
    }

    /**
     * POST /api/v1/game/challenge
     */
    async getChallenge(req, res, next) {
        try {
            const challenge = crypto.randomUUID();
            // Store challenge with 5 minute expiration
            activeChallenges.set(challenge, Date.now() + 5 * 60 * 1000);
            return res.status(200).json({
                status: "SUCCESS",
                challenge
            });
        } catch (error) {
            next(error);
        }
    }

    /**
     * POST /api/v1/game/initiate
     */
    async initiate(req, res, next) {
        try {
            const { challenge } = req.body;
            if (!challenge) {
                return res.status(400).json({ status: "ERROR", message: "Missing challenge." });
            }

            const expiry = activeChallenges.get(challenge);
            if (!expiry || Date.now() > expiry) {
                if (expiry) activeChallenges.delete(challenge);
                return res.status(403).json({ status: "ERROR", message: "Invalid or expired challenge." });
            }
            activeChallenges.delete(challenge);

            const tokenPayload = res.locals.integrityPayload || null;
            if (!tokenPayload) {
                return res.status(403).json({ status: "ERROR", message: "Missing integrity token payload." });
            }

            // Verify content binding
            const serverRequestHash = cryptoService.computePayloadHash(req.body);
            if (serverRequestHash !== tokenPayload.requestDetails?.requestHash) {
                return res.status(403).json({ status: "ERROR", message: "Payload signature validation failed." });
            }

            const verdicts = gamePolicy.evaluateEnvironment(tokenPayload);
            const sessionId = crypto.randomUUID();

            const targetTime = parseFloat((Math.random() * (30.0 - 10.0) + 10.0).toFixed(2));

            /*
             * TOCTOU DEFENCE: RANDOM INSTRUCTION INTERVALS
             *
             * A common flaw in mobile attestation is the binary "check-at-login" pattern.
             * A user could pass the initial integrity check on a pristine device, and once
             * inside the game loop, turn on a malicious overlay or screen-recording cheat engine.
             *
             * To defeat this Time-of-Check to Time-of-Use (TOCTOU) attack, the server generates
             * randomized check-in timestamps. The client requests and caches Play Integrity API
             * tokens in the background for each specified interval to prove continuous environment security.
             *
             * IMPORTANT NOTE ON TOKEN EXPIRATION: Play Integrity API express tokens must be decoded
             * within 10 minutes of creation, otherwise they will fail to be decoded. Because this
             * sample game session is designed to last a maximum of 30 seconds, the client can safely
             * cache these tokens and send them to the server at the end of the session without
             * risking expiration.
             */
            const intervals = [];
            let currentOffset = 2.0;
            while (currentOffset < targetTime - 2.0) {
                currentOffset += parseFloat((Math.random() * (5.0 - 2.0) + 2.0).toFixed(2));
                if (currentOffset < targetTime) {
                    intervals.push(parseFloat(currentOffset.toFixed(2)));
                }
            }

            activeSessions.set(sessionId, {
                sessionId,
                targetTime,
                intervals,
                serverStartTime: Date.now(),
                // If the token was missing/invalid, this guarantees the score is rejected later
                hasInitSecurityViolation: !verdicts.isSecure
            });

            return res.status(StatusCodes.OK).json({
                status: "SUCCESS",
                sessionId,
                targetTime,
                intervals,
                checklist: verdicts
            });

        } catch (error) {
            next(error);
        }
    }

    /**
     * POST /api/v1/game/status
     */
    async getStatus(req, res, next) {
        try {
            // Same as initiate: allow status checks even if the token failed locally.
            // DEVELOPER NOTE: The request hash is not verified here because the status check is 
            // read-only and non-sensitive. Do not use unverified nonces on critical transaction endpoints.
            const tokenPayload = res.locals.integrityPayload || null;
            const verdicts = gamePolicy.evaluateEnvironment(tokenPayload);

            return res.status(StatusCodes.OK).json({
                status: "SUCCESS",
                checklist: verdicts
            });
        } catch (error) {
            next(error);
        }
    }

    /**
     * POST /api/v1/game/stop
     * Orchestrates the final session verifications.
     */
    async stop(req, res, next) {
        try {
            const { sessionId, clientStartTime, actualTime, intervalTokens } = req.body;
            const finalTokenPayload = res.locals.integrityPayload || null;

            const session = activeSessions.get(sessionId);
            if (!session) return res.status(StatusCodes.NOT_FOUND).json({ status: "ERROR", message: "Session expired." });

            activeSessions.delete(sessionId);

            // Reject the score if they reached the end without a valid final token
            if (!finalTokenPayload) {
                return res.status(StatusCodes.FORBIDDEN).json({ status: "ERROR", message: "Environment compromised: Invalid final attestation." });
            }

            // 1. Verify standard payload integrity (Content Binding)
            if (!this.#verifyFinalPayloadHash(req.body, finalTokenPayload)) {
                return res.status(StatusCodes.FORBIDDEN).json({ status: "ERROR", message: "Payload signature validation failed." });
            }

            // 2. Verify all background interval tokens
            const intervalError = await this.#verifyIntervalTokens(
                session.intervals,
                actualTime,
                intervalTokens,
                sessionId,
                clientStartTime
            );

            if (intervalError) {
                return res.status(StatusCodes.FORBIDDEN).json(intervalError);
            }

            // 3. Final environmental check
            if (session.hasInitSecurityViolation || !this.#verifyFinalEnvironment(finalTokenPayload)) {
                return res.status(StatusCodes.FORBIDDEN).json({ status: "ERROR", message: "Environment compromised." });
            }

            return res.status(StatusCodes.OK).json({ status: "SUCCESS", message: "Score verified." });

        } catch (error) {
            next(error);
        }
    }

    /**
     * Validates that the body's computed hash matches the token's signed request hash.
     */
    #verifyFinalPayloadHash(body, finalTokenPayload) {
        const serverRequestHash = cryptoService.computePayloadHash(body);
        return serverRequestHash === finalTokenPayload.requestDetails?.requestHash;
    }

    /**
     * Loops through every expected interval to ensure the environment remained secure.
     * Returns an error object if compromised, or null if perfectly secure.
     */
    async #verifyIntervalTokens(sessionIntervals, actualTime, intervalTokens, sessionId, clientStartTime) {
        const expectedIntervals = sessionIntervals.filter(interval => interval <= actualTime);
        const receivedTokens = intervalTokens || [];

        for (const expectedInterval of expectedIntervals) {
            const match = receivedTokens.find(rt => rt.interval === expectedInterval);

            if (!match) {
                return {
                    status: "ERROR",
                    message: `Security violation: Missing background token for interval ${expectedInterval}s.`
                };
            }

            let decodedIntervalPayload;
            try {
                decodedIntervalPayload = await cryptoService.decodeToken(match.token);
            } catch (e) {
                return { status: "ERROR", message: "Failed to verify interval attestation." };
            }

            const expectedHashSource = `${sessionId}${clientStartTime}${expectedInterval}`;
            const computedHash = crypto.createHash('sha256').update(expectedHashSource).digest('hex');

            if (decodedIntervalPayload.requestDetails?.requestHash !== computedHash) {
                return { status: "ERROR", message: "Interval binding mismatch." };
            }

            const intervalVerdicts = gamePolicy.evaluateEnvironment(decodedIntervalPayload);
            if (!intervalVerdicts.isSecure) {
                return {
                    status: "ERROR",
                    error_code: "ENVIRONMENT_COMPROMISED",
                    message: `Cheat toggling detected: Environment compromised at interval ${expectedInterval}s.`
                };
            }
        }

        return null; // Signifies all intervals passed
    }

    /**
     * Evaluates the policy on the final closing payload.
     */
    #verifyFinalEnvironment(finalTokenPayload) {
        const finalVerdicts = gamePolicy.evaluateEnvironment(finalTokenPayload);
        return finalVerdicts.isSecure;
    }
}

module.exports = new GameController();