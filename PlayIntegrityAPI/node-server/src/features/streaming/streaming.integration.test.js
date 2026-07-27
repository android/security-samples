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
const app = require('../../../app');
const { StatusCodes } = require('http-status-codes');

const cryptoService = require('../../services/crypto.service');
const integrityService = require('../../services/integrity.service');
const manifestService = require('./manifest.service');

const { STREAM_QUALITIES } = require('./streaming.constants');
const { HEADERS, INTEGRITY_VERDICTS } = require('../../config/constants');

jest.mock('../../services/integrity.service', () => ({
    decodeToken: jest.fn()
}));

jest.mock('./manifest.service', () => ({
    getFilteredManifest: jest.fn()
}));

describe('Streaming Feature Integration Tests (GET /api/v1/streaming/:contentId/manifest.mpd)', () => {

    const CONTENT_ID = 'sample_video_01';
    const ENDPOINT = `/api/v1/streaming/${CONTENT_ID}/manifest.mpd`;

    beforeEach(() => {
        // Dynamically reflect the requested quality back in the payload
        // to verify full pipeline delivery to the client.
        manifestService.getFilteredManifest.mockImplementation((quality) => {
            return Promise.resolve(`<MPD>Tier:${quality}</MPD>`);
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should return 200 but gracefully degrade to RESTRICTED (144p) if token header is missing', async () => {
        const response = await request(app)
            .get(ENDPOINT);

        expect(response.status).toBe(StatusCodes.OK);
        expect(response.headers['content-type']).toContain('application/dash+xml');
        expect(integrityService.decodeToken).not.toHaveBeenCalled();
        expect(manifestService.getFilteredManifest).toHaveBeenCalledWith(STREAM_QUALITIES.RESTRICTED);
        expect(response.text).toBe(`<MPD>Tier:${STREAM_QUALITIES.RESTRICTED}</MPD>`);
    });

    it('should return 200 and PREMIUM (1080p) manifest for a perfectly valid, strong integrity device', async () => {
        const payload = { action: 'fetch_manifest', contentId: CONTENT_ID };
        const expectedHash = cryptoService.computePayloadHash(payload);

        integrityService.decodeToken.mockResolvedValue({
            requestDetails: { requestHash: expectedHash },
            deviceIntegrity: {
                deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY, INTEGRITY_VERDICTS.DEVICE.MEETS_STRONG_INTEGRITY],
                deviceAttributes: {
                    sdkVersion: 33
                }
            },
            appIntegrity: {
                appRecognitionVerdict: INTEGRITY_VERDICTS.APP.PLAY_RECOGNIZED
            },
            accountDetails: {
                appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.LICENSED
            }
        });

        const response = await request(app)
            .get(ENDPOINT)
            .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_mock_token');

        expect(response.status).toBe(StatusCodes.OK);
        expect(manifestService.getFilteredManifest).toHaveBeenCalledWith(STREAM_QUALITIES.PREMIUM);
        expect(response.text).toBe(`<MPD>Tier:${STREAM_QUALITIES.PREMIUM}</MPD>`);
    });

    it('should return 200 but gracefully degrade to RESTRICTED (144p) if Content Binding hash mismatches', async () => {
        const tamperedHash = cryptoService.computePayloadHash({ action: 'fetch_manifest', contentId: 'DIFFERENT_VIDEO' });
        integrityService.decodeToken.mockResolvedValue({
            requestDetails: { requestHash: tamperedHash },
            deviceIntegrity: {
                deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_STRONG_INTEGRITY],
                deviceAttributes: {
                    sdkVersion: 33
                }
            },
            appIntegrity: {
                appRecognitionVerdict: INTEGRITY_VERDICTS.APP.PLAY_RECOGNIZED
            },
            accountDetails: {
                appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.LICENSED
            }
        });

        const response = await request(app)
            .get(ENDPOINT)
            .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_mock_token');

        expect(response.status).toBe(StatusCodes.OK);
        expect(manifestService.getFilteredManifest).toHaveBeenCalledWith(STREAM_QUALITIES.RESTRICTED);
        expect(response.text).toBe(`<MPD>Tier:${STREAM_QUALITIES.RESTRICTED}</MPD>`);
    });

    it('should return 200 and BASIC (240p) manifest if the policy evaluates to basic integrity', async () => {
        const payload = { action: 'fetch_manifest', contentId: CONTENT_ID };
        const expectedHash = cryptoService.computePayloadHash(payload);
        integrityService.decodeToken.mockResolvedValue({
            requestDetails: { requestHash: expectedHash },
            deviceIntegrity: {
                deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_BASIC_INTEGRITY],
                deviceAttributes: {
                    sdkVersion: 33
                }
            },
            appIntegrity: {
                appRecognitionVerdict: INTEGRITY_VERDICTS.APP.PLAY_RECOGNIZED
            },
            accountDetails: {
                appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.LICENSED
            }
        });

        const response = await request(app)
            .get(ENDPOINT)
            .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_mock_token');

        expect(response.status).toBe(StatusCodes.OK);
        expect(manifestService.getFilteredManifest).toHaveBeenCalledWith(STREAM_QUALITIES.BASIC);
        expect(response.text).toBe(`<MPD>Tier:${STREAM_QUALITIES.BASIC}</MPD>`);
    });
});