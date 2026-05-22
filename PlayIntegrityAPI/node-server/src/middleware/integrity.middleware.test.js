const extractIntegrityToken = require('./integrity.middleware');
const integrityService = require('../services/integrity.service');
const { HEADERS } = require('../config/constants');

jest.mock('../services/integrity.service', () => ({
    decodeToken: jest.fn()
}));

describe('Integrity Middleware Unit Tests', () => {
    let req, res, next;

    beforeEach(() => {
        // Reset the mock request, response, and next function before each test
        req = {
            headers: {}
        };

        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn(),
            locals: {}
        };

        next = jest.fn();

        jest.spyOn(console, 'warn').mockImplementation(() => {});
        jest.clearAllMocks();
    });

    afterEach(() => {
        console.warn.mockRestore();
    });

    it('should attach null to locals and call next() if the token header is missing', async () => {
        await extractIntegrityToken(req, res, next);

        expect(res.locals.integrityPayload).toBeNull();
        expect(next).toHaveBeenCalledTimes(1);
        expect(res.status).not.toHaveBeenCalled();
        expect(res.json).not.toHaveBeenCalled();
        expect(integrityService.decodeToken).not.toHaveBeenCalled();
    });

    it('should attach null to locals, log a warning, and call next() if the token decode fails', async () => {
        req.headers[HEADERS.PLAY_INTEGRITY_TOKEN] = 'invalid_fake_token';
        integrityService.decodeToken.mockRejectedValue(new Error('Token decryption failed'));

        await extractIntegrityToken(req, res, next);


        expect(console.warn).toHaveBeenCalledWith(expect.stringContaining('Token decode failed'));
        expect(res.locals.integrityPayload).toBeNull();
        expect(next).toHaveBeenCalledTimes(1);
        expect(res.status).not.toHaveBeenCalled();
    });

    it('should attach the decoded payload to locals and call next() if the token is valid', async () => {
        const mockDecodedPayload = { deviceIntegrity: 'MEETS_BASIC_INTEGRITY' };
        req.headers[HEADERS.PLAY_INTEGRITY_TOKEN] = 'valid_fake_token';
        integrityService.decodeToken.mockResolvedValue(mockDecodedPayload);

        await extractIntegrityToken(req, res, next);

        expect(integrityService.decodeToken).toHaveBeenCalledWith('valid_fake_token');
        expect(res.locals.integrityPayload).toEqual(mockDecodedPayload);
        expect(next).toHaveBeenCalledTimes(1);
        expect(res.status).not.toHaveBeenCalled();
    });
});