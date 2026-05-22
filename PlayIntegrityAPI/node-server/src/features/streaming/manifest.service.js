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

const axios = require('axios');
const xml2js = require('xml2js');

const ROOT_MANIFEST_URL = 'https://pia-canonical-sample-app.web.app/media/manifest.mpd';
const MEDIA_BASE_URL = 'https://pia-canonical-sample-app.web.app/media/';

/**
 * Service responsible for dynamically creating the DASH media manifest for the stream.
 *
 * PRODUCTION SECURITY NOTE:
 * While Play Integrity API protects the manifest endpoint, a determined attacker could
 * use a single trusted device to harvest a "premium" manifest and then share that text file
 * with a botnet. To prevent those bots from downloading the heavy video chunks and
 * driving up CDN egress costs, this service could implement CDN URL Signing.
 *
 * In production, the media chunks must also be protected by Widevine DRM on Android, ensuring
 * that even if chunks are downloaded, they cannot be decrypted without a hardware-backed CDM.
 */
class ManifestService {
    async getFilteredManifest(maxQuality) {
        const rawXml = await this.#fetchRootManifest();
        const parsedManifest = await this.#parseXml(rawXml);

        this.#injectBaseUrl(parsedManifest);
        this.#filterVideoRepresentations(parsedManifest, maxQuality);

        return this.#buildXml(parsedManifest);
    }

    async #fetchRootManifest() {
        const response = await axios.get(ROOT_MANIFEST_URL);
        return response.data;
    }

    async #parseXml(xmlString) {
        const parser = new xml2js.Parser();
        return await parser.parseStringPromise(xmlString);
    }

    /**
     * Injects the root media URL into the manifest.
     *
     * PRODUCTION IMPLEMENTATION:
     * To implement CDN URL Signing, the MEDIA_BASE_URL injected here should not be static.
     * Instead, you would dynamically generate a cryptographically signed URL bound to the
     * requesting user with a short expiration time (TTL).
     *
     * Example:
     * <BaseURL>https://media.example.com/auth-[timestamp]-[signature]/</BaseURL>
     *
     * If an attacker harvests the manifest and sends it to a botnet, the CDN will see
     * that the bot's credentials do not match the signature in the BaseURL and will
     * instantly drop the request with a 403 Forbidden, saving your egress bandwidth.
     *
     * Target XML Structure:
     * <MPD>
     *    <ProgramInformation>...</ProgramInformation>
     *    <ServiceDescription>...</ServiceDescription>
     *    <!-- Injecting BaseURL here -->
     *    <BaseURL>https://pia-canonical-sample-app.web.app/media/</BaseURL>
     *    <Period>...</Period>
     * </MPD>
     */
    #injectBaseUrl(parsedManifest) {
        if (!parsedManifest?.MPD) return;

        // Use destructuring to extract elements we want to position specifically
        // and '...otherFields' to capture everything else dynamically.
        const { $, Period, ...otherFields } = parsedManifest.MPD;

        parsedManifest.MPD = {
            $,
            ...otherFields,
            BaseURL: [MEDIA_BASE_URL],
            Period
        };
    }

    /**
     * Strips out premium video resolutions if the device fails integrity checks.
     *
     * Target XML Structure:
     * <MPD>
     *    <Period>
     *       <AdaptationSet contentType="video">
     *          <Representation height="1080">...</Representation> <-- Removed if maxQuality < 1080
     *          <Representation height="720">...</Representation>  <-- Kept if maxQuality >= 720
     *          <Representation height="480">...</Representation>  <-- Kept
     *       </AdaptationSet>
     *    </Period>
     * </MPD>
     */
    #filterVideoRepresentations(parsedManifest, maxQuality) {
        const period = parsedManifest.MPD.Period?.[0];
        if (!period || !period.AdaptationSet) return;

        // Find all adaptation sets that look like video (e.g., multiple codecs like H.264 and VP9)
        const videoSets = period.AdaptationSet.filter(set =>
            set.$?.contentType === 'video' ||
            set.Representation?.some(r => r.$?.height)
        );

        if (videoSets.length === 0) {
            console.warn('[ManifestService] No video adaptation sets found to filter.');
            return;
        }

        // Apply quality capping to every video set found
        videoSets.forEach(videoSet => {
            videoSet.Representation = videoSet.Representation.filter(rep => {
                const height = parseInt(rep.$?.height, 10);
                return !isNaN(height) && height <= maxQuality;
            });
        });
    }

    #buildXml(parsedManifest) {
        const builder = new xml2js.Builder();
        return builder.buildObject(parsedManifest);
    }
}

module.exports = new ManifestService();