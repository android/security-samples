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

const { StatusCodes } = require('http-status-codes');
const bankController = require('./bank.controller');
const cryptoService = require('../../services/crypto.service');
const bankPolicy = require('./bank.policy');

jest.mock('../../services/crypto.service');
jest.mock('./bank.policy');

describe('BankController Unit Tests', () => {
    let req, res, next;

    beforeEach(() => {
        req = {
            body: {
                accountNumber: "1234567890",
                amount: "50.00",
                idempotencyKey: `test-key-${Date.now()}-${Math.random()}`
            }
        };

        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn(),
            locals: {}
        };

        next = jest.fn();

        jest.clearAllMocks();
    });

    it('should return 400 MISSING_IDEMPOTENCY_KEY if key is missing', async () => {
        delete req.body.idempotencyKey;
        cryptoService.computePayloadHash.mockReturnValue('matching_hash');
        res.locals.integrityPayload = {
            requestDetails: { requestHash: 'matching_hash' }
        };
        bankPolicy.evaluateTransferPolicy.mockReturnValue(true);

        await bankController.handleTransfer(req, res, next);

        expect(res.status).toHaveBeenCalledWith(StatusCodes.BAD_REQUEST);
        expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
            error_code: "MISSING_IDEMPOTENCY_KEY"
        }));
    });

    it('should return 409 DUPLICATE_TRANSACTION if key was already processed', async () => {
        res.locals.integrityPayload = { requestDetails: { requestHash: 'hash' } };
        cryptoService.computePayloadHash.mockReturnValue('hash');
        bankPolicy.evaluateTransferPolicy.mockReturnValue(true);

        await bankController.handleTransfer(req, res, next);
        expect(res.status).toHaveBeenCalledWith(StatusCodes.OK);

        // Run again with the exact same request body
        const duplicateRes = { 
            status: jest.fn().mockReturnThis(), 
            json: jest.fn(),
            locals: { integrityPayload: { requestDetails: { requestHash: 'hash' } } }
        };
        await bankController.handleTransfer(req, duplicateRes, next);

        expect(duplicateRes.status).toHaveBeenCalledWith(StatusCodes.CONFLICT);
        expect(duplicateRes.json).toHaveBeenCalledWith(expect.objectContaining({
            error_code: "DUPLICATE_TRANSACTION"
        }));
    });

    it('should return 401 UNAUTHORIZED if token payload is missing', async () => {
        res.locals.integrityPayload = null;

        await bankController.handleTransfer(req, res, next);

        expect(res.status).toHaveBeenCalledWith(StatusCodes.UNAUTHORIZED);
        expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
            error_code: "UNAUTHORIZED"
        }));
    });

    it('should return 403 REQUEST_TAMPERED if hashes do not match', async () => {
        cryptoService.computePayloadHash.mockReturnValue('server_hash_abc');
        res.locals.integrityPayload = {
            requestDetails: { requestHash: 'different_token_hash_xyz' }
        };

        await bankController.handleTransfer(req, res, next);

        expect(res.status).toHaveBeenCalledWith(StatusCodes.FORBIDDEN);
        expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
            error_code: "REQUEST_TAMPERED"
        }));
    });

    it('should return 403 INTEGRITY_REJECTED if policy fails', async () => {
        cryptoService.computePayloadHash.mockReturnValue('matching_hash');
        res.locals.integrityPayload = {
            requestDetails: { requestHash: 'matching_hash' }
        };
        bankPolicy.evaluateTransferPolicy.mockReturnValue(false);

        await bankController.handleTransfer(req, res, next);

        expect(res.status).toHaveBeenCalledWith(StatusCodes.FORBIDDEN);
        expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
            error_code: "INTEGRITY_REJECTED",
            remediation_action: "GET_INTEGRITY"
        }));
    });

    it('should return 200 SUCCESS if hashes match and policy passes', async () => {
        cryptoService.computePayloadHash.mockReturnValue('matching_hash');
        res.locals.integrityPayload = {
            requestDetails: { requestHash: 'matching_hash' }
        };
        bankPolicy.evaluateTransferPolicy.mockReturnValue(true);

        await bankController.handleTransfer(req, res, next);

        expect(res.status).toHaveBeenCalledWith(StatusCodes.OK);
        expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
            status: "SUCCESS"
        }));
    });

    it('should call next(error) if an exception is thrown', async () => {
        const mockError = new Error("Network failure");
        res.locals.integrityPayload = { requestDetails: {} };
        cryptoService.computePayloadHash.mockImplementation(() => {
            throw mockError;
        });

        await bankController.handleTransfer(req, res, next);

        expect(next).toHaveBeenCalledWith(mockError);
    });
});