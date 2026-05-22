const manifestService = require('./manifest.service');
const xml2js = require('xml2js');
const axios = require('axios'); // Required to mock specific manifest structures for edge cases

/**
 * Helper method to parse the raw XML manifest and extract all video representations.
 * Updated to safely extract across multiple video adaptation sets using duck typing.
 *
 * @param {string} manifestXml - The raw XML string returned by the service.
 * @returns {Promise<Array|undefined>} - An array of video Representation objects, or undefined if none found.
 */
async function extractVideoRepresentations(manifestXml) {
    const parser = new xml2js.Parser();
    const parsedManifest = await parser.parseStringPromise(manifestXml);

    const period = parsedManifest.MPD?.Period?.[0];
    if (!period || !period.AdaptationSet) return undefined;

    const videoSets = period.AdaptationSet.filter(set =>
        set.$?.contentType === 'video' ||
        set.Representation?.some(r => r.$?.height)
    );

    if (videoSets.length === 0) return undefined;

    return videoSets.flatMap(set => set.Representation);
}

describe('ManifestService (Live Integration Tests)', () => {
    jest.setTimeout(10000);

    it('should fetch the actual manifest and filter out streams above 480p', async () => {
        const maxHeight = 480;

        const customManifestXml = await manifestService.getFilteredManifest(maxHeight);
        const representations = await extractVideoRepresentations(customManifestXml);

        // Verify the raw response is a populated string
        expect(typeof customManifestXml).toBe('string');
        expect(customManifestXml.length).toBeGreaterThan(0);
        // Verify the representations array exists and wasn't completely emptied
        expect(representations).toBeDefined();
        expect(representations.length).toBeGreaterThan(0);
        // Verify that absolutely NO representation exceeds the 480p maximum
        representations.forEach(rep => {
            const height = parseInt(rep.$.height, 10);
            expect(height).toBeLessThanOrEqual(maxHeight);
        });
        // Explicitly verify a known higher resolution (1080p) is genuinely missing
        const has1080p = representations.some(rep => parseInt(rep.$.height, 10) === 1080);
        expect(has1080p).toBe(false);
    });

    it('should fetch the actual manifest and retain streams up to 1080p', async () => {
        const maxHeight = 1080;

        const customManifestXml = await manifestService.getFilteredManifest(maxHeight);
        const representations = await extractVideoRepresentations(customManifestXml);

        expect(representations).toBeDefined();
        expect(representations.length).toBeGreaterThan(0);
        // Verify the 1080p representation successfully survived the filter
        const has1080p = representations.some(rep => parseInt(rep.$.height, 10) === 1080);
        expect(has1080p).toBe(true);
        // Verify no stray representations exceed the 1080p maximum
        representations.forEach(rep => {
            const height = parseInt(rep.$.height, 10);
            expect(height).toBeLessThanOrEqual(maxHeight);
        });
    });
});

describe('ManifestService (Edge Case Unit Tests)', () => {
    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('should identify and filter video sets using duck-typing when contentType="video" is missing', async () => {
        const mockManifest = `
            <MPD>
                <Period>
                    <AdaptationSet>
                        <Representation height="1080" bandwidth="4000000"/>
                        <Representation height="480" bandwidth="1000000"/>
                    </AdaptationSet>
                </Period>
            </MPD>
        `;
        jest.spyOn(axios, 'get').mockResolvedValue({ data: mockManifest });

        const resultXml = await manifestService.getFilteredManifest(480);
        const representations = await extractVideoRepresentations(resultXml);

        expect(representations).toBeDefined();
        expect(representations.length).toBe(1);
        expect(representations[0].$.height).toBe("480");
    });

    it('should apply filtering to multiple video adaptation sets (e.g. multiple codecs)', async () => {
        const mockManifest = `
            <MPD>
                <Period>
                    <!-- H.264 Video Set -->
                    <AdaptationSet contentType="video" codecs="avc1">
                        <Representation height="1080" bandwidth="4000000"/>
                        <Representation height="480" bandwidth="1000000"/>
                    </AdaptationSet>
                    <!-- VP9 Video Set -->
                    <AdaptationSet contentType="video" codecs="vp09">
                        <Representation height="1080" bandwidth="3000000"/>
                        <Representation height="480" bandwidth="800000"/>
                    </AdaptationSet>
                </Period>
            </MPD>
        `;
        jest.spyOn(axios, 'get').mockResolvedValue({ data: mockManifest });

        const resultXml = await manifestService.getFilteredManifest(480);
        const representations = await extractVideoRepresentations(resultXml);

        // Should extract remaining representations from BOTH sets
        expect(representations).toBeDefined();
        expect(representations.length).toBe(2);
        expect(representations.every(rep => parseInt(rep.$.height, 10) <= 480)).toBe(true);
    });

    it('should handle manifests with no video adaptation sets gracefully without throwing errors', async () => {
        const mockManifest = `
            <MPD>
                <Period>
                    <!-- Audio only -->
                    <AdaptationSet contentType="audio">
                        <Representation bandwidth="128000"/>
                    </AdaptationSet>
                </Period>
            </MPD>
        `;
        jest.spyOn(axios, 'get').mockResolvedValue({ data: mockManifest });
        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

        // Execution should not throw
        const resultXml = await manifestService.getFilteredManifest(480);
        const representations = await extractVideoRepresentations(resultXml);

        expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('No video adaptation sets found'));
        expect(representations).toBeUndefined(); // Helper returns undefined if no video sets exist
    });
});