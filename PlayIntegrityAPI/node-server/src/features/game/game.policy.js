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