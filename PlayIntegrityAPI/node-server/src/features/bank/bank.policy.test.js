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