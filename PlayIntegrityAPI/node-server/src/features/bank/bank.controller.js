const cryptoService = require('../../services/crypto.service');
const bankPolicy = require('./bank.policy');

/**
 * Controller for handling Bank micro-app endpoints.
 */
class BankController {

    /**
     * Handles the POST request for a bank transfer.
     *
     * @param {Object} req - Express request object containing the payload and headers.
     * @param {Object} res - Express response object used to send verdicts or success data.
     * @param {Function} next - Express middleware function for error delegation.
     */
    async handleTransfer(req, res, next) {
        try {
            const payload = req.body;
            // Access the payload attached by the integrity middleware
            const tokenPayload = res.locals.integrityPayload;
            if (!tokenPayload) {
                return res.status(401).json({
                    status: "ERROR",
                    error_code: "UNAUTHORIZED",
                    message: "A valid Play Integrity token is required for the transaction."
                });
            }

            // Compute payload hash for Content Binding verification
            const serverRequestHash = cryptoService.computePayloadHash(payload);

            // Verify Content Binding
            const tokenRequestHash = tokenPayload.requestDetails?.requestHash;
            if (serverRequestHash !== tokenRequestHash) {
                return res.status(403).json({
                    status: "ERROR",
                    error_code: "REQUEST_TAMPERED",
                    message: "The request payload has been altered."
                });
            }

            // Evaluate verdicts against the Bank feature policy
            const isPolicyMet = bankPolicy.evaluateTransferPolicy(tokenPayload);

            if (!isPolicyMet) {
                return res.status(403).json({
                    status: "ERROR",
                    error_code: "INTEGRITY_REJECTED",
                    message: "Device does not meet the required security standards.",
                    remediation_code: 4,
                    remediation_action: "GET_INTEGRITY"
                });
            }

            // Happy Path: Process transaction
            return res.status(200).json({
                status: "SUCCESS",
                transactionId: `TXN-${Math.floor(Math.random() * 1000000000)}`,
                message: "Transfer completed successfully."
            });

        } catch (error) {
            next(error);
        }
    }
}

module.exports = new BankController();