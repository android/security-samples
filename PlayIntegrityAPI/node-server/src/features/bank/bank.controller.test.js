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

const bankController = require('./bank.controller');
const cryptoService = require('../../services/crypto.service');
const bankPolicy = require('./bank.policy');

jest.mock('../../services/crypto.service');
jest.mock('./bank.policy');

describe('BankController Unit Tests', () => {
    let req, res, next;

    beforeEach(() => {
        req = {
            body: { accountNumber: "1234567890", amount: "50.00" }
        };

        res = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn(),
            locals: {}
        };

        next = jest.fn();

        jest.clearAllMocks();
    });

    it('should return 401 UNAUTHORIZED if token payload is missing', async () => {
        res.locals.integrityPayload = null;

        await bankController.handleTransfer(req, res, next);

        expect(res.status).toHaveBeenCalledWith(401);
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

        expect(res.status).toHaveBeenCalledWith(403);
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

        expect(res.status).toHaveBeenCalledWith(403);
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

        expect(res.status).toHaveBeenCalledWith(200);
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