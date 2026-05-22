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

const {STREAM_QUALITIES} = require('./streaming.constants');
const {INTEGRITY_VERDICTS} = require('../../config/constants');

/**
 * Evaluates Play Integrity verdicts to determine the maximum allowed video resolution.
 * Returns the max height matching the DASH manifest.
 */
exports.evaluateStreamQuality = (tokenPayload) => {
    const deviceIntegrity = tokenPayload.deviceIntegrity?.deviceRecognitionVerdict || [];
    const sdkVersion = tokenPayload.deviceIntegrity?.deviceAttributes?.sdkVersion || 0;

    if (tokenPayload.requestDetails?.requestPackageName !== process.env.PACKAGE_NAME || deviceIntegrity.length === 0) {
        return STREAM_QUALITIES.RESTRICTED;
    }

    if (deviceIntegrity.includes(INTEGRITY_VERDICTS.DEVICE.MEETS_STRONG_INTEGRITY)) {
        if (sdkVersion >= 33) {
            return STREAM_QUALITIES.PREMIUM;
        }
        return STREAM_QUALITIES.HIGH;
    }

    if (deviceIntegrity.includes(INTEGRITY_VERDICTS.DEVICE.MEETS_DEVICE_INTEGRITY)) {
        if (sdkVersion >= 33) {
            return STREAM_QUALITIES.HIGH;
        }
        return STREAM_QUALITIES.STANDARD;
    }

    if (deviceIntegrity.includes(INTEGRITY_VERDICTS.DEVICE.MEETS_VIRTUAL_INTEGRITY)) {
        return STREAM_QUALITIES.STANDARD;
    }

    if (deviceIntegrity.includes(INTEGRITY_VERDICTS.DEVICE.MEETS_BASIC_INTEGRITY)) {
        return STREAM_QUALITIES.BASIC;
    }

    // Fallback to Restricted Tier
    return STREAM_QUALITIES.RESTRICTED;
};