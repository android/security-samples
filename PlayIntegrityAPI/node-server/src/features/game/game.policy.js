const { INTEGRITY_VERDICTS } = require('../../config/constants');

/**
 * Policy rules for the Game micro-app.
 */
class GamePolicy {

    /**
     * Evaluates the Play Integrity payload's environment details.
     */
    evaluateEnvironment(payload) {
        if (!payload || payload.requestDetails?.requestPackageName !== process.env.PACKAGE_NAME) {
            return {
                isSecure: false,
                screenCaptureSafe: false,
                accessibilitySafe: false,
                playProtectSafe: false
            };
        }
        const isAppRecognised = payload.appIntegrity?.appRecognitionVerdict === INTEGRITY_VERDICTS.APP.PLAY_RECOGNIZED;
        const isLicenced = payload.accountDetails?.appLicensingVerdict === INTEGRITY_VERDICTS.ACCOUNT.LICENSED;
        const playProtectSafe = payload.environmentDetails?.playProtectVerdict === INTEGRITY_VERDICTS.ENVIRONMENT.PLAY_PROTECT.NO_ISSUES;

        let screenCaptureSafe = false;
        let accessibilitySafe = false;

        const appAccessRisk = payload.environmentDetails?.appAccessRiskVerdict;
        if (appAccessRisk && Object.keys(appAccessRisk).length > 0) {
            const appsDetected = payload.environmentDetails.appAccessRiskVerdict.appsDetected;

            // Only flag negatively if an unknown app is found, i.e, an app not recognized by Play
            // Evaluate App Access Risk: Screen Capture
            screenCaptureSafe = !appsDetected.some(status =>
                status === INTEGRITY_VERDICTS.ENVIRONMENT.APP_ACCESS_RISK.UNKNOWN_CAPTURING
            );

            // Evaluate App Access Risk: Accessibility / Overlays
            accessibilitySafe = !appsDetected.some(status =>
                status === INTEGRITY_VERDICTS.ENVIRONMENT.APP_ACCESS_RISK.UNKNOWN_OVERLAYS ||
                status === INTEGRITY_VERDICTS.ENVIRONMENT.APP_ACCESS_RISK.UNKNOWN_CONTROLLING
            );
        }

        const isSecure = isAppRecognised && isLicenced &&
            playProtectSafe && screenCaptureSafe && accessibilitySafe;

        return {
            isSecure,
            screenCaptureSafe,
            accessibilitySafe,
            playProtectSafe
        };
    }
}

module.exports = new GamePolicy();