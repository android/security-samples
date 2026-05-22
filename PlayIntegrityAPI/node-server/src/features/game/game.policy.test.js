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

const gamePolicy = require('./game.policy');
const { INTEGRITY_VERDICTS } = require('../../config/constants');

describe('GamePolicy.evaluateEnvironment', () => {

    beforeAll(() => {
        process.env.PACKAGE_NAME = 'com.test.app';
    });

    const getValidPayload = () => ({
        requestDetails: {
            requestPackageName: 'com.test.app'
        },
        deviceIntegrity: {
            deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY]
        },
        appIntegrity: {
            appRecognitionVerdict: INTEGRITY_VERDICTS.APP.PLAY_RECOGNIZED
        },
        accountDetails: {
            appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.LICENSED
        },
        environmentDetails: {
            playProtectVerdict: INTEGRITY_VERDICTS.ENVIRONMENT.PLAY_PROTECT.NO_ISSUES,
            appAccessRiskVerdict: {
                appsDetected: [INTEGRITY_VERDICTS.ENVIRONMENT.APP_ACCESS_RISK.KNOWN_INSTALLED]
            }
        }
    });

    describe('Empty / Missing Payload or Invalid Package', () => {
        it('should return all false values when payload is falsy', () => {
            const result = gamePolicy.evaluateEnvironment(null);
            expect(result).toEqual({
                isSecure: false,
                screenCaptureSafe: false,
                accessibilitySafe: false,
                playProtectSafe: false
            });
        });

        it('should return all false values when requestPackageName does not match expected package', () => {
            const payload = getValidPayload();
            payload.requestDetails.requestPackageName = 'com.fake.app';

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result).toEqual({
                isSecure: false,
                screenCaptureSafe: false,
                accessibilitySafe: false,
                playProtectSafe: false
            });
        });
    });

    describe('Fully Secure Environment', () => {
        it('should return all true values when all checks pass and appsDetected is empty', () => {
            const result = gamePolicy.evaluateEnvironment(getValidPayload());

            expect(result).toEqual({
                isSecure: true,
                screenCaptureSafe: true,
                accessibilitySafe: true,
                playProtectSafe: true
            });
        });
    });

    describe('Missing Environment Data (Strict Fallback)', () => {
        it('should return false for risk checks if environmentDetails is completely missing', () => {
            const payload = getValidPayload();
            delete payload.environmentDetails;

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result.screenCaptureSafe).toBe(false);
            expect(result.accessibilitySafe).toBe(false);
            expect(result.playProtectSafe).toBe(false);
            expect(result.isSecure).toBe(false);
        });

        it('should return false for risk checks if appAccessRiskVerdict is empty', () => {
            const payload = getValidPayload();
            delete payload.environmentDetails.appAccessRiskVerdict.appsDetected;

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result.screenCaptureSafe).toBe(false);
            expect(result.accessibilitySafe).toBe(false);
            expect(result.isSecure).toBe(false);
            expect(result.playProtectSafe).toBe(true);
        });
    });

    describe('Core Invisible Checks (isSecure Failures)', () => {
        it('should return isSecure as false if app is UNRECOGNIZED', () => {
            const payload = getValidPayload();
            payload.appIntegrity.appRecognitionVerdict = INTEGRITY_VERDICTS.APP.UNRECOGNIZED_VERSION;

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result.isSecure).toBe(false);
            expect(result.playProtectSafe).toBe(true);
        });

        it('should return isSecure as false if the app is UNLICENSED', () => {
            const payload = getValidPayload();
            payload.accountDetails.appLicensingVerdict = INTEGRITY_VERDICTS.ACCOUNT.UNLICENSED;

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result.isSecure).toBe(false);
        });
    });

    describe('App Access Risk Array (appsDetected)', () => {
        it('should return screenCaptureSafe as false if an unknown capturing app is detected', () => {
            const payload = getValidPayload();
            payload.environmentDetails.appAccessRiskVerdict.appsDetected = [
                INTEGRITY_VERDICTS.ENVIRONMENT.APP_ACCESS_RISK.UNKNOWN_CAPTURING
            ];

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result.screenCaptureSafe).toBe(false);
            expect(result.accessibilitySafe).toBe(true);
            expect(result.isSecure).toBe(false);
        });

        it('should return accessibilitySafe as false if an unknown controlling app is detected', () => {
            const payload = getValidPayload();
            payload.environmentDetails.appAccessRiskVerdict.appsDetected = [
                INTEGRITY_VERDICTS.ENVIRONMENT.APP_ACCESS_RISK.UNKNOWN_CONTROLLING
            ];

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result.accessibilitySafe).toBe(false);
            expect(result.isSecure).toBe(false);
        });

        it('should return accessibilitySafe as true if an known controlling app is detected', () => {
            const payload = getValidPayload();
            payload.environmentDetails.appAccessRiskVerdict.appsDetected = [
                INTEGRITY_VERDICTS.ENVIRONMENT.APP_ACCESS_RISK.KNOWN_CONTROLLING
            ];

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result.accessibilitySafe).toBe(true);
            expect(result.isSecure).toBe(true);
        });


        it('should fail multiple risk checks if multiple threats are in the array', () => {
            const payload = getValidPayload();
            payload.environmentDetails.appAccessRiskVerdict.appsDetected = [
                INTEGRITY_VERDICTS.ENVIRONMENT.APP_ACCESS_RISK.UNKNOWN_CAPTURING,
                INTEGRITY_VERDICTS.ENVIRONMENT.APP_ACCESS_RISK.UNKNOWN_CONTROLLING
            ];

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result.accessibilitySafe).toBe(false);
            expect(result.screenCaptureSafe).toBe(false);
            expect(result.isSecure).toBe(false);
        });
    });

    describe('Play Protect Checks', () => {
        it('should return playProtectSafe as false if Play Protect finds issues', () => {
            const payload = getValidPayload();
            payload.environmentDetails.playProtectVerdict = INTEGRITY_VERDICTS.ENVIRONMENT.PLAY_PROTECT.HIGH_RISK;

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result.playProtectSafe).toBe(false);
            expect(result.isSecure).toBe(false);
        });

        it('should return playProtectSafe as false if Play Protect verdict is unevaluated', () => {
            const payload = getValidPayload();
            payload.environmentDetails.playProtectVerdict = INTEGRITY_VERDICTS.ENVIRONMENT.PLAY_PROTECT.UNEVALUATED;

            const result = gamePolicy.evaluateEnvironment(payload);

            expect(result.playProtectSafe).toBe(false);
            expect(result.isSecure).toBe(false);
        });
    });
});