const { evaluateStreamQuality } = require('./streaming.policy');
const { STREAM_QUALITIES } = require('./streaming.constants');
const { INTEGRITY_VERDICTS } = require('../../config/constants');

describe('Streaming Policy: evaluateStreamQuality', () => {

    beforeAll(() => {
        process.env.PACKAGE_NAME = 'com.test.app';
    });

    describe('Restricted Tier (Immediate Failures)', () => {

        it('should return RESTRICTED when requestPackageName does not match expected package', () => {
            const verdicts = {
                requestDetails: {
                    requestPackageName: 'com.malicious.app'
                },
                deviceIntegrity: {
                    deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_STRONG_INTEGRITY],
                    deviceAttributes: {
                        sdkVersion: 33
                    }
                }
            };

            const result = evaluateStreamQuality(verdicts);

            expect(result).toBe(STREAM_QUALITIES.RESTRICTED);
        });

        it('should return RESTRICTED when device integrity labels are empty', () => {
            const verdicts = {
                requestDetails: { requestPackageName: 'com.test.app' },
                deviceIntegrity: {
                    deviceRecognitionVerdict: [],
                    deviceAttributes: {
                        sdkVersion: 33
                    }
                }
            };

            const result = evaluateStreamQuality(verdicts);

            expect(result).toBe(STREAM_QUALITIES.RESTRICTED);
        });
    });

    describe('Premium & High Quality (MEETS_STRONG_INTEGRITY)', () => {

        it('should return PREMIUM when strong integrity is met on Android 13 (SDK 33+)', () => {
            const verdicts = {
                requestDetails: { requestPackageName: 'com.test.app' },
                deviceIntegrity: {
                    deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY, INTEGRITY_VERDICTS.DEVICE.MEETS_STRONG_INTEGRITY],
                    deviceAttributes: {
                        sdkVersion: 33
                    }
                }
            };

            const result = evaluateStreamQuality(verdicts);

            expect(result).toBe(STREAM_QUALITIES.PREMIUM);
        });

        it('should return HIGH when strong integrity is met but device is older than Android 13', () => {
            const verdicts = {
                requestDetails: { requestPackageName: 'com.test.app' },
                deviceIntegrity: {
                    deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY, INTEGRITY_VERDICTS.DEVICE.MEETS_STRONG_INTEGRITY],
                    deviceAttributes: {
                        sdkVersion: 32
                    }
                }
            };

            const result = evaluateStreamQuality(verdicts);

            expect(result).toBe(STREAM_QUALITIES.HIGH);
        });
    });

    describe('High Quality & Standard (MEETS_DEVICE_INTEGRITY or MEETS_VIRTUAL_INTEGRITY)', () => {

        it('should return HIGH when device integrity is met on Android 13 (SDK 33+)', () => {
            const verdicts = {
                requestDetails: { requestPackageName: 'com.test.app' },
                deviceIntegrity: {
                    deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY],
                    deviceAttributes: {
                        sdkVersion: 34
                    }
                }
            };

            const result = evaluateStreamQuality(verdicts);

            expect(result).toBe(STREAM_QUALITIES.HIGH);
        });

        it('should return STANDARD when virtual integrity is met regardless of SDK version (e.g. SDK 34)', () => {
            const verdicts = {
                requestDetails: { requestPackageName: 'com.test.app' },
                deviceIntegrity: {
                    deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_VIRTUAL_INTEGRITY],
                    deviceAttributes: {
                        sdkVersion: 34
                    }
                }
            };

            const result = evaluateStreamQuality(verdicts);

            expect(result).toBe(STREAM_QUALITIES.STANDARD);
        });

        it('should return STANDARD when device integrity is met but device is older than Android 13', () => {
            const verdicts = {
                requestDetails: { requestPackageName: 'com.test.app' },
                deviceIntegrity: {
                    deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY],
                    deviceAttributes: {
                        sdkVersion: 30
                    }
                }
            };

            const result = evaluateStreamQuality(verdicts);

            expect(result).toBe(STREAM_QUALITIES.STANDARD);
        });
    });

    describe('Basic Tier (MEETS_BASIC_INTEGRITY)', () => {

        it('should return BASIC when only basic integrity is met regardless of SDK version', () => {
            const verdicts = {
                requestDetails: { requestPackageName: 'com.test.app' },
                deviceIntegrity: {
                    deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_BASIC_INTEGRITY],
                    deviceAttributes: {
                        sdkVersion: 35
                    }
                }
            };

            const result = evaluateStreamQuality(verdicts);

            expect(result).toBe(STREAM_QUALITIES.BASIC);
        });
    });

    describe('Edge Cases & Fallbacks', () => {

        it('should default to SDK version 0 and evaluate accordingly if device attributes is missing', () => {
            const verdicts = {
                requestDetails: { requestPackageName: 'com.test.app' },
                deviceIntegrity: { deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY] },
            };

            const result = evaluateStreamQuality(verdicts);

            expect(result).toBe(STREAM_QUALITIES.STANDARD);
        });

        it('should fallback to RESTRICTED if integrity labels are completely missing', () => {
            const verdicts = {
                requestDetails: { requestPackageName: 'com.test.app' },
                deviceIntegrity: {
                    deviceRecognitionVerdict: [],
                    deviceAttributes: {
                        sdkVersion: 33
                    }
                }
            };

            const result = evaluateStreamQuality(verdicts);

            expect(result).toBe(STREAM_QUALITIES.RESTRICTED);
        });
    });
});