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

const request = require('supertest');
const crypto = require('crypto');
const app = require('../../../app');

const cryptoService = require('../../services/crypto.service');
const integrityService = require('../../services/integrity.service');
const { HEADERS, INTEGRITY_VERDICTS } = require('../../config/constants');

jest.mock('../../services/integrity.service', () => ({
    decodeToken: jest.fn()
}));

jest.mock('../../services/crypto.service', () => {
    const actualCryptoService = jest.requireActual('../../services/crypto.service');
    actualCryptoService.decodeToken = jest.fn();
    return actualCryptoService;
});

describe('Game Feature Integration Tests', () => {

    const getSecurePayload = (requestHash) => ({
        requestDetails: { requestHash },
        appIntegrity: {
            appRecognitionVerdict: INTEGRITY_VERDICTS.APP.PLAY_RECOGNIZED
        },
        accountDetails: {
            appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.LICENSED
        },
        environmentDetails: {
            playProtectVerdict: INTEGRITY_VERDICTS.ENVIRONMENT.PLAY_PROTECT.NO_ISSUES,
            appAccessRiskVerdict: {
                appsDetected: []
            }
        }
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /api/v1/game/initiate', () => {
        it('should return 200 and an insecure checklist if token header is missing', async () => {
            const response = await request(app)
                .post('/api/v1/game/initiate')
                .send({});

            expect(response.status).toBe(200);
            expect(response.body.status).toBe('SUCCESS');
            expect(response.body.sessionId).toBeDefined();
            expect(response.body.checklist.isSecure).toBe(false);
            expect(integrityService.decodeToken).not.toHaveBeenCalled();
        });

        it('should return 200 and a secure checklist if a valid token is provided', async () => {
            integrityService.decodeToken.mockResolvedValue(getSecurePayload('dummy_hash'));

            const response = await request(app)
                .post('/api/v1/game/initiate')
                .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_mock_token')
                .send({});

            expect(response.status).toBe(200);
            expect(response.body.status).toBe('SUCCESS');
            expect(response.body.checklist.isSecure).toBe(true);
        });
    });

    describe('POST /api/v1/game/status', () => {
        it('should return 200 and reflect a secure environment state when valid token is supplied', async () => {
            integrityService.decodeToken.mockResolvedValue(getSecurePayload('dummy_hash'));

            const response = await request(app)
                .post('/api/v1/game/status')
                .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_mock_token')
                .send({});

            expect(response.status).toBe(200);
            expect(response.body.status).toBe('SUCCESS');
            expect(response.body.checklist.isSecure).toBe(true);
        });
    });

    describe('POST /api/v1/game/stop (Full Flow & Security Checks)', () => {
        it('should successfully complete and verify a perfectly secure game session', async () => {
            integrityService.decodeToken.mockResolvedValue(getSecurePayload('init_hash'));

            const initRes = await request(app)
                .post('/api/v1/game/initiate')
                .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_init_token')
                .send({});

            const { sessionId, intervals, targetTime } = initRes.body;
            const clientStartTime = 1000;
            const actualTime = targetTime + 1.0;

            const stopPayload = {
                sessionId,
                clientStartTime,
                actualTime,
                intervalTokens: intervals.map(interval => ({
                    interval,
                    token: `mock_interval_token_${interval}`
                }))
            };

            const expectedFinalHash = cryptoService.computePayloadHash(stopPayload);
            integrityService.decodeToken.mockResolvedValue(getSecurePayload(expectedFinalHash));

            cryptoService.decodeToken.mockImplementation(async (token) => {
                const intervalStr = token.replace('mock_interval_token_', '');
                const interval = parseFloat(intervalStr);
                const expectedHashSource = `${sessionId}${clientStartTime}${interval}`;
                const expectedHash = crypto.createHash('sha256').update(expectedHashSource).digest('hex');

                return getSecurePayload(expectedHash);
            });

            const stopRes = await request(app)
                .post('/api/v1/game/stop')
                .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_final_token')
                .send(stopPayload);

            expect(stopRes.status).toBe(200);
            expect(stopRes.body.status).toBe('SUCCESS');
            expect(stopRes.body.message).toBe('Score verified.');
        });

        it('should return 403 if background interval tokens are missing entirely', async () => {
            integrityService.decodeToken.mockResolvedValue(getSecurePayload('init_hash'));
            const initRes = await request(app)
                .post('/api/v1/game/initiate')
                .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_init_token')
                .send({});

            const { sessionId, targetTime } = initRes.body;
            const stopPayload = {
                sessionId,
                clientStartTime: 1000,
                actualTime: targetTime + 1.0,
                intervalTokens: []
            };

            const expectedFinalHash = cryptoService.computePayloadHash(stopPayload);
            integrityService.decodeToken.mockResolvedValue(getSecurePayload(expectedFinalHash));

            const stopRes = await request(app)
                .post('/api/v1/game/stop')
                .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_final_token')
                .send(stopPayload);

            expect(stopRes.status).toBe(403);
            expect(stopRes.body.message).toMatch(/Security violation: Missing background token/);
        });

        it('should return 403 if the final request payload has been tampered with (Content Binding Hash Mismatch)', async () => {
            integrityService.decodeToken.mockResolvedValue(getSecurePayload('init_hash'));
            const initRes = await request(app)
                .post('/api/v1/game/initiate')
                .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_init_token')
                .send({});

            const { sessionId, targetTime, intervals } = initRes.body;
            const stopPayload = {
                sessionId,
                clientStartTime: 1000,
                actualTime: targetTime + 1.0,
                intervalTokens: intervals.map(interval => ({ interval, token: 'mock' }))
            };

            integrityService.decodeToken.mockResolvedValue(getSecurePayload('tampered_hash_12345'));

            const stopRes = await request(app)
                .post('/api/v1/game/stop')
                .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_final_token')
                .send(stopPayload);

            expect(stopRes.status).toBe(403);
            expect(stopRes.body.message).toBe('Payload signature validation failed.');
        });

        it('should return 404 if the submitted session ID does not exist', async () => {
            const stopPayload = {
                sessionId: crypto.randomUUID(),
                clientStartTime: 1000,
                actualTime: 15.0,
                intervalTokens: []
            };

            integrityService.decodeToken.mockResolvedValue(getSecurePayload('mock'));

            const stopRes = await request(app)
                .post('/api/v1/game/stop')
                .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_final_token')
                .send(stopPayload);

            expect(stopRes.status).toBe(404);
            expect(stopRes.body.message).toBe('Session expired.');
        });
    });
});