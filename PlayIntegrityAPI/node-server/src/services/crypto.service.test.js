const cryptoService = require('./crypto.service');

describe('CryptoService.computePayloadHash', () => {

    const diversePayloads = [
        { name: 'Bank Transfer', data: { accountNumber: "1234567890", amount: "50.00" } },
        { name: 'Login Request', data: { username: "player1", timestamp: 1713550000 } },
        { name: 'Deeply Nested Object', data: { user: { id: 42, preferences: { dark_mode: true } } } },
        { name: 'Empty Object', data: {} },
        { name: 'Array Payload', data: ["item1", "item2", 3, 4] }
    ];

    it.each(diversePayloads)('should generate a valid base64url hash for a $name payload', ({ data }) => {
        const hash = cryptoService.computePayloadHash(data);

        expect(typeof hash).toBe('string');
        expect(hash.length).toBeGreaterThan(0);
        // base64url compliance check: must NOT contain '+', '/', or padding '='
        expect(hash).not.toMatch(/[+/=]/);
    });

    it('should consistently generate the same hash for the exact same payload', () => {
        const payload = { action: "unlock_achievement", id: 99 };

        const hash1 = cryptoService.computePayloadHash(payload);
        const hash2 = cryptoService.computePayloadHash(payload);

        expect(hash1).toBe(hash2);
    });

    it('should generate different hashes when even a tiny part of the payload changes', () => {
        const validPayload = { level: 5, score: 1000 };
        const tamperedPayload = { level: 5, score: 9999 };

        const validHash = cryptoService.computePayloadHash(validPayload);
        const tamperedHash = cryptoService.computePayloadHash(tamperedPayload);

        expect(validHash).not.toBe(tamperedHash);
    });
});