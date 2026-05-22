const crypto = require('crypto');
const gamePolicy = require('./game.policy');
const cryptoService = require('../../services/crypto.service');

// In-memory store for active sessions
const activeSessions = new Map();

class GameController {

    constructor() {
        this.initiate = this.initiate.bind(this);
        this.getStatus = this.getStatus.bind(this);
        this.stop = this.stop.bind(this);
    }

    /**
     * POST /api/v1/game/initiate
     */
    async initiate(req, res, next) {
        try {
            const tokenPayload = res.locals.integrityPayload || null;
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

            return res.status(200).json({
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
            // Same as initiate: allow status checks even if the token failed locally
            const tokenPayload = res.locals.integrityPayload || null;
            const verdicts = gamePolicy.evaluateEnvironment(tokenPayload);

            return res.status(200).json({
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
            if (!session) return res.status(404).json({ status: "ERROR", message: "Session expired." });

            activeSessions.delete(sessionId);

            // Reject the score if they reached the end without a valid final token
            if (!finalTokenPayload) {
                return res.status(403).json({ status: "ERROR", message: "Environment compromised: Invalid final attestation." });
            }

            // 1. Verify standard payload integrity (Content Binding)
            if (!this.#verifyFinalPayloadHash(req.body, finalTokenPayload)) {
                return res.status(403).json({ status: "ERROR", message: "Payload signature validation failed." });
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
                return res.status(403).json(intervalError);
            }

            // 3. Final environmental check
            if (session.hasInitSecurityViolation || !this.#verifyFinalEnvironment(finalTokenPayload)) {
                return res.status(403).json({ status: "ERROR", message: "Environment compromised." });
            }

            return res.status(200).json({ status: "SUCCESS", message: "Score verified." });

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