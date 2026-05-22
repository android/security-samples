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

process.env.PACKAGE_NAME = 'com.example.testapp';
process.env.GOOGLE_CREDENTIALS_PATH = './dummy-credentials.json';

const { google } = require('googleapis');

jest.mock('googleapis', () => {
    const mockDecodeIntegrityToken = jest.fn();
    return {
        google: {
            auth: {
                GoogleAuth: jest.fn().mockImplementation(() => {
                    return {}; // Returns a dummy auth client object
                })
            },
            playintegrity: jest.fn(() => ({
                v1: {
                    decodeIntegrityToken: mockDecodeIntegrityToken
                }
            }))
        }
    };
});

const integrityService = require('./integrity.service');

describe('IntegrityService.decodeToken', () => {
    let mockDecode;

    beforeEach(() => {
        mockDecode = google.playintegrity().v1.decodeIntegrityToken;
        jest.clearAllMocks();
    });

    it('should successfully decode and return the token payload', async () => {
        const expectedPayload = { requestDetails: { requestHash: "hash123" } };
        mockDecode.mockResolvedValue({
            data: { tokenPayloadExternal: expectedPayload }
        });

        const result = await integrityService.decodeToken('valid_token_123');

        expect(mockDecode).toHaveBeenCalledWith({
            packageName: 'com.example.testapp',
            requestBody: { integrityToken: 'valid_token_123' }
        });
        expect(result).toEqual(expectedPayload);
    });

    it('should throw a custom error if Google API fails', async () => {
        jest.spyOn(console, 'error').mockImplementation(() => {});

        mockDecode.mockRejectedValue(new Error('Google API Error'));

        await expect(integrityService.decodeToken('invalid_token'))
            .rejects
            .toThrow("FAILED_TO_DECODE_TOKEN");

        console.error.mockRestore();
    });
});