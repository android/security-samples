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

const bankPolicy = require('./bank.policy');
const { INTEGRITY_VERDICTS } = require('../../config/constants');

describe('BankPolicy.evaluateTransferPolicy', () => {
    const EXPECTED_PACKAGE = 'com.example.bankapp';

    beforeAll(() => {
        process.env.PACKAGE_NAME = EXPECTED_PACKAGE;
    });

    afterAll(() => {
        delete process.env.PACKAGE_NAME;
    });

    it('should return true when all strict requirements are met', () => {
        const validPayload = {
            deviceIntegrity: {
                deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY]
            },
            appIntegrity: {
                appRecognitionVerdict: INTEGRITY_VERDICTS.APP.PLAY_RECOGNIZED
            },
            accountDetails: {
                appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.LICENSED
            },
            requestDetails: {
                requestPackageName: EXPECTED_PACKAGE
            }
        };
        expect(bankPolicy.evaluateTransferPolicy(validPayload)).toBe(true);
    });

    it('should return false if app is UNRECOGNIZED', () => {
        const payload = {
            deviceIntegrity: {
                deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY]
            },
            appIntegrity: {
                appRecognitionVerdict: INTEGRITY_VERDICTS.APP.UNRECOGNIZED_VERSION
            },
            accountDetails: {
                appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.LICENSED
            },
            requestDetails: {
                requestPackageName: EXPECTED_PACKAGE
            }
        };
        expect(bankPolicy.evaluateTransferPolicy(payload)).toBe(false);
    });

    it('should return false if device fails basic integrity', () => {
        const payload = {
            deviceIntegrity: {
                deviceRecognitionVerdict: []
            },
            appIntegrity: {
                appRecognitionVerdict: INTEGRITY_VERDICTS.APP.UNEVALUATED
            },
            accountDetails: {
                appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.UNEVALUATED
            },
            requestDetails: {
                requestPackageName: EXPECTED_PACKAGE
            }
        };
        expect(bankPolicy.evaluateTransferPolicy(payload)).toBe(false);
    });

    it('should return false if the requesting package name does not match the server expectation', () => {
        const payload = {
            deviceIntegrity: {
                deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY]
            },
            appIntegrity: {
                appRecognitionVerdict: INTEGRITY_VERDICTS.APP.PLAY_RECOGNIZED
            },
            accountDetails: {
                appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.LICENSED
            },
            requestDetails: {
                requestPackageName: 'com.malicious.spoofedapp'
            }
        };
        expect(bankPolicy.evaluateTransferPolicy(payload)).toBe(false);
    });
});