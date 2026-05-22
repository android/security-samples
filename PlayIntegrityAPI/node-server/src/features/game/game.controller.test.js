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

const gameController = require('./game.controller');
const gamePolicy = require('./game.policy');
const cryptoService = require('../../services/crypto.service');
const crypto = require('crypto');

jest.mock('./game.policy');
jest.mock('../../services/crypto.service', () => ({
    computePayloadHash: jest.fn(),
    decodeToken: jest.fn()
}));

describe('GameController Unit Tests', () => {
    let req, res, next;

    const createValidSession = async (isSecure = true) => {
        gamePolicy.evaluateEnvironment.mockReturnValue({ isSecure, playProtectSafe: true });

        const initReq = { body: {} };
        const initRes = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn(),
            locals: { integrityPayload: { dummy: 'token' } }
        };

        await gameController.initiate(initReq, initRes, jest.fn());
        return initRes.json.mock.calls[0][0]; // Extract { sessionId, targetTime, intervals, checklist }
    };

    beforeEach(() => {
        req = {
            body: {}
        };

        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn(),
            locals: {}
        };

        next = jest.fn();

        jest.clearAllMocks();
    });

    describe('POST /initiate', () => {
        it('should create a session, calculate intervals, and return 200 SUCCESS', async () => {
            gamePolicy.evaluateEnvironment.mockReturnValue({ isSecure: true });
            res.locals.integrityPayload = { some: 'payload' };

            await gameController.initiate(req, res, next);

            expect(gamePolicy.evaluateEnvironment).toHaveBeenCalledWith({ some: 'payload' });
            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                status: "SUCCESS",
                sessionId: expect.any(String),
                targetTime: expect.any(Number),
                intervals: expect.any(Array),
                checklist: { isSecure: true }
            }));
        });

        it('should call next(error) if an exception occurs', async () => {
            const mockError = new Error("System error");
            gamePolicy.evaluateEnvironment.mockImplementation(() => {
                throw mockError;
            });

            await gameController.initiate(req, res, next);

            expect(next).toHaveBeenCalledWith(mockError);
        });
    });

    describe('POST /status', () => {
        it('should evaluate the environment and return 200 SUCCESS with the checklist', async () => {
            gamePolicy.evaluateEnvironment.mockReturnValue({ isSecure: false });
            res.locals.integrityPayload = null;

            await gameController.getStatus(req, res, next);

            expect(gamePolicy.evaluateEnvironment).toHaveBeenCalledWith(null);
            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith({
                status: "SUCCESS",
                checklist: { isSecure: false }
            });
        });

        it('should call next(error) if an exception occurs', async () => {
            const mockError = new Error("Network error");
            gamePolicy.evaluateEnvironment.mockImplementation(() => {
                throw mockError;
            });

            await gameController.getStatus(req, res, next);

            expect(next).toHaveBeenCalledWith(mockError);
        });
    });

    describe('POST /stop', () => {
        it('should return 404 if the session does not exist', async () => {
            req.body.sessionId = 'non-existent-uuid';

            await gameController.stop(req, res, next);

            expect(res.status).toHaveBeenCalledWith(404);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                message: "Session expired."
            }));
        });

        it('should return 403 if the final token payload is missing', async () => {
            const { sessionId } = await createValidSession();
            req.body.sessionId = sessionId;
            res.locals.integrityPayload = null; // Missing token

            await gameController.stop(req, res, next);

            expect(res.status).toHaveBeenCalledWith(403);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                message: "Environment compromised: Invalid final attestation."
            }));
        });

        it('should return 403 if payload signature validation fails', async () => {
            const { sessionId } = await createValidSession();
            req.body.sessionId = sessionId;
            cryptoService.computePayloadHash.mockReturnValue('server_hash');
            res.locals.integrityPayload = {
                requestDetails: { requestHash: 'different_token_hash' }
            };

            await gameController.stop(req, res, next);

            expect(res.status).toHaveBeenCalledWith(403);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                message: "Payload signature validation failed."
            }));
        });

        it('should return 403 if a background token for an expected interval is missing', async () => {
            const { sessionId, intervals, targetTime } = await createValidSession();
            const serverHash = 'matching_hash';
            cryptoService.computePayloadHash.mockReturnValue(serverHash);
            res.locals.integrityPayload = { requestDetails: { requestHash: serverHash } };
            req.body = {
                sessionId,
                actualTime: targetTime,
                intervalTokens: [] // Missing tokens
            };

            await gameController.stop(req, res, next);

            expect(res.status).toHaveBeenCalledWith(403);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                message: expect.stringContaining("Security violation: Missing background token")
            }));
        });

        it('should return 403 if interval attestation fails to decode', async () => {
            const { sessionId, intervals, targetTime } = await createValidSession();
            const serverHash = 'matching_hash';
            cryptoService.computePayloadHash.mockReturnValue(serverHash);
            res.locals.integrityPayload = { requestDetails: { requestHash: serverHash } };
            req.body = {
                sessionId,
                actualTime: targetTime,
                intervalTokens: intervals.map(interval => ({ interval, token: 'bad_token' }))
            };
            cryptoService.decodeToken.mockRejectedValue(new Error("Decode failed"));

            await gameController.stop(req, res, next);

            expect(res.status).toHaveBeenCalledWith(403);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                message: "Failed to verify interval attestation."
            }));
        });

        it('should return 403 if interval binding mismatch occurs (wrong hash)', async () => {
            const { sessionId, intervals, targetTime } = await createValidSession();
            const serverHash = 'matching_hash';
            cryptoService.computePayloadHash.mockReturnValue(serverHash);
            res.locals.integrityPayload = { requestDetails: { requestHash: serverHash } };
            req.body = {
                sessionId,
                clientStartTime: 1000,
                actualTime: targetTime,
                intervalTokens: intervals.map(interval => ({ interval, token: `token_${interval}` }))
            };
            cryptoService.decodeToken.mockResolvedValue({
                requestDetails: { requestHash: 'tampered_interval_hash' }
            });

            await gameController.stop(req, res, next);

            expect(res.status).toHaveBeenCalledWith(403);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                message: "Interval binding mismatch."
            }));
        });

        it('should return 403 if cheat toggling is detected (interval policy fails)', async () => {
            const { sessionId, intervals, targetTime } = await createValidSession();
            const clientStartTime = 1000;
            const serverHash = 'matching_hash';
            cryptoService.computePayloadHash.mockReturnValue(serverHash);
            res.locals.integrityPayload = { requestDetails: { requestHash: serverHash } };
            req.body = {
                sessionId,
                clientStartTime,
                actualTime: targetTime,
                intervalTokens: intervals.map(interval => ({ interval, token: `token_${interval}` }))
            };
            cryptoService.decodeToken.mockImplementation(async (token) => {
                const interval = parseFloat(token.split('_')[1]);
                const expectedHashSource = `${sessionId}${clientStartTime}${interval}`;
                return {
                    requestDetails: { requestHash: crypto.createHash('sha256').update(expectedHashSource).digest('hex') }
                };
            });
            // Simulate policy failing mid-game
            gamePolicy.evaluateEnvironment.mockReturnValue({ isSecure: false });

            await gameController.stop(req, res, next);

            expect(res.status).toHaveBeenCalledWith(403);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                error_code: "ENVIRONMENT_COMPROMISED",
                message: expect.stringContaining("Cheat toggling detected")
            }));
        });

        it('should return 403 if the session had a mid-game violation recorded at initiation', async () => {
            const { sessionId, intervals, targetTime } = await createValidSession(false);
            const clientStartTime = 1000;
            const serverHash = 'matching_hash';
            cryptoService.computePayloadHash.mockReturnValue(serverHash);
            res.locals.integrityPayload = { requestDetails: { requestHash: serverHash } };
            req.body = {
                sessionId,
                clientStartTime,
                actualTime: targetTime,
                intervalTokens: intervals.map(interval => ({ interval, token: `token_${interval}` }))
            };
            cryptoService.decodeToken.mockImplementation(async (token) => {
                const interval = parseFloat(token.split('_')[1]);
                const expectedHashSource = `${sessionId}${clientStartTime}${interval}`;
                return {
                    requestDetails: { requestHash: crypto.createHash('sha256').update(expectedHashSource).digest('hex') }
                };
            });
            gamePolicy.evaluateEnvironment.mockReturnValue({ isSecure: true });

            await gameController.stop(req, res, next);

            expect(res.status).toHaveBeenCalledWith(403);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                message: "Environment compromised."
            }));
        });

        it('should return 200 SUCCESS when everything passes perfectly', async () => {
            const { sessionId, intervals, targetTime } = await createValidSession(true);
            const clientStartTime = 1000;
            const serverHash = 'matching_hash';
            cryptoService.computePayloadHash.mockReturnValue(serverHash);
            res.locals.integrityPayload = { requestDetails: { requestHash: serverHash } };
            req.body = {
                sessionId,
                clientStartTime,
                actualTime: targetTime + 1.0, // Ensures all expected intervals are parsed
                intervalTokens: intervals.map(interval => ({ interval, token: `token_${interval}` }))
            };
            cryptoService.decodeToken.mockImplementation(async (token) => {
                const interval = parseFloat(token.split('_')[1]);
                const expectedHashSource = `${sessionId}${clientStartTime}${interval}`;
                return {
                    requestDetails: { requestHash: crypto.createHash('sha256').update(expectedHashSource).digest('hex') }
                };
            });
            gamePolicy.evaluateEnvironment.mockReturnValue({ isSecure: true });

            await gameController.stop(req, res, next);

            expect(res.status).toHaveBeenCalledWith(200);
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
                status: "SUCCESS",
                message: "Score verified."
            }));
        });
    });
});