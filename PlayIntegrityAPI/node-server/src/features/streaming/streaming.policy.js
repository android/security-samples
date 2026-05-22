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