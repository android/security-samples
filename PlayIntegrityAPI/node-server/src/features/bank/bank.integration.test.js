const request = require('supertest');
const app = require('../../../app');

const cryptoService = require('../../services/crypto.service');
const integrityService = require('../../services/integrity.service');

const { HEADERS, INTEGRITY_VERDICTS } = require('../../config/constants');

jest.mock('../../services/integrity.service', () => ({
    decodeToken: jest.fn()
}));

describe('Bank Feature Integration Tests (POST /api/v1/bank/transfer)', () => {

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should return 401 if token header is missing', async () => {
        const response = await request(app)
            .post('/api/v1/bank/transfer')
            .send({ accountNumber: "1234567890", amount: "50.00" });

        expect(response.status).toBe(401);
        expect(response.body.error_code).toBe("UNAUTHORIZED");
        expect(response.body.message).toBe("A valid Play Integrity token is required for the transaction.");
        expect(integrityService.decodeToken).not.toHaveBeenCalled();
    });

    it('should return 200 SUCCESS for a perfectly valid transfer', async () => {
        const payload = { accountNumber: "1234567890", amount: "50.00" };
        const expectedHash = cryptoService.computePayloadHash(payload);
        integrityService.decodeToken.mockResolvedValue({
            requestDetails: { requestHash: expectedHash },
            deviceIntegrity: {
                deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY]
            },
            appIntegrity: {
                appRecognitionVerdict: INTEGRITY_VERDICTS.APP.PLAY_RECOGNIZED
            },
            accountDetails: {
                appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.LICENSED
            }
        });

        const response = await request(app)
            .post('/api/v1/bank/transfer')
            .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_mock_token')
            .send(payload);

        expect(response.status).toBe(200);
        expect(response.body.status).toBe("SUCCESS");
        expect(response.body.transactionId).toBeDefined();
    });

    it('should return 403 REQUEST_TAMPERED if the payload is altered', async () => {
        const payload = { accountNumber: "1234567890", amount: "99.99" };
        const originalSafeHash = cryptoService.computePayloadHash({ accountNumber: "1234567890", amount: "50.00" });
        integrityService.decodeToken.mockResolvedValue({
            requestDetails: { requestHash: originalSafeHash },
            deviceIntegrity: {
                deviceRecognitionVerdict: [INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY]
            },
            appIntegrity: {
                appRecognitionVerdict: INTEGRITY_VERDICTS.APP.PLAY_RECOGNIZED
            },
            accountDetails: {
                appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.LICENSED
            }
        });

        const response = await request(app)
            .post('/api/v1/bank/transfer')
            .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_mock_token')
            .send(payload);

        expect(response.status).toBe(403);
        expect(response.body.error_code).toBe("REQUEST_TAMPERED");
    });

    it('should return 403 INTEGRITY_REJECTED if the policy fails', async () => {
        const payload = { accountNumber: "1234567890", amount: "50.00" };
        const expectedHash = cryptoService.computePayloadHash(payload);
        integrityService.decodeToken.mockResolvedValue({
            requestDetails: { requestHash: expectedHash },
            deviceIntegrity: {
                deviceRecognitionVerdict: []
            },
            appIntegrity: {
                appRecognitionVerdict: INTEGRITY_VERDICTS.APP.UNEVALUATED
            },
            accountDetails: {
                appLicensingVerdict: INTEGRITY_VERDICTS.ACCOUNT.UNEVALUATED
            }
        });

        const response = await request(app)
            .post('/api/v1/bank/transfer')
            .set(HEADERS.PLAY_INTEGRITY_TOKEN, 'valid_mock_token')
            .send(payload);

        expect(response.status).toBe(403);
        expect(response.body.error_code).toBe("INTEGRITY_REJECTED");
    });
});