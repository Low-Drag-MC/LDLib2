package com.lowdragmc.lowdraglib2.client.font.glyph;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistanceTransformTest {

    private static final int ON_EDGE = DistanceTransform.ON_EDGE;

    private static byte[] filledRect(int width, int height) {
        var mask = new byte[width * height];
        Arrays.fill(mask, (byte) 0xFF);
        return mask;
    }

    private static int value(byte[] field, int width, int x, int y) {
        return field[y * width + x] & 0xFF;
    }

    /**
     * The signed distance the encoding represents at a given byte value.
     */
    private static float distance(int encoded, float spread) {
        return (encoded - ON_EDGE) * spread / ON_EDGE;
    }

    @Test
    void producesThePaddedSize() {
        var padding = 3;
        var field = DistanceTransform.fromMask(filledRect(4, 5), 4, 5, padding, padding);
        assertEquals((4 + padding * 2) * (5 + padding * 2), field.length);
    }

    @Test
    void isPositiveInsideAndNegativeOutside() {
        var padding = 4;
        var width = 10;
        var height = 10;
        var field = DistanceTransform.fromMask(filledRect(width, height), width, height, padding, padding);
        var fieldWidth = width + padding * 2;

        // deep inside the rectangle
        assertTrue(value(field, fieldWidth, padding + 5, padding + 5) > ON_EDGE);
        // in the padding, well outside the shape
        assertTrue(value(field, fieldWidth, 0, 0) < ON_EDGE);
    }

    @Test
    void distancesMatchTheAnalyticSolutionForARectangle() {
        var padding = 6;
        var spread = (float) padding;
        var width = 16;
        var height = 16;
        var field = DistanceTransform.fromMask(filledRect(width, height), width, height, padding, padding);
        var fieldWidth = width + padding * 2;

        // walk straight out from the left edge of the rectangle along its middle row
        var y = padding + height / 2;
        for (int step = 1; step <= 4; step++) {
            var x = padding - step;
            var expected = -(float) step;
            var actual = distance(value(field, fieldWidth, x, y), spread);
            assertEquals(expected, actual, 1.0f,
                    "distance " + step + " pixels left of the edge should be about " + expected);
        }
        // and inwards from the same edge
        for (int step = 1; step <= 4; step++) {
            var x = padding + step - 1;
            var expected = (float) step;
            var actual = distance(value(field, fieldWidth, x, y), spread);
            assertEquals(expected, actual, 1.0f,
                    "distance " + step + " pixels inside the edge should be about " + expected);
        }
    }

    @Test
    void distancesMatchTheAnalyticSolutionForADisc() {
        var padding = 8;
        var spread = (float) padding;
        var size = 40;
        var radius = 15f;
        var centre = (size - 1) / 2f;
        var mask = new byte[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                var dx = x - centre;
                var dy = y - centre;
                mask[y * size + x] = (byte) (Math.sqrt(dx * dx + dy * dy) <= radius ? 0xFF : 0);
            }
        }
        var field = DistanceTransform.fromMask(mask, size, size, padding, padding);
        var fieldWidth = size + padding * 2;

        // sample along the horizontal centre line, comparing against radius - |x - centre|
        var y = padding + Math.round(centre);
        for (int x = padding; x < padding + size; x++) {
            var expected = radius - Math.abs((x - padding) - centre);
            if (Math.abs(expected) > spread - 1) {
                continue; // outside the representable range, the field clamps there
            }
            var actual = distance(value(field, fieldWidth, x, y), spread);
            assertEquals(expected, actual, 1.5f, "disc distance at x=" + (x - padding));
        }
    }

    /**
     * Anti aliased coverage of a disc, computed by supersampling, which is what FreeType hands back for an
     * outline.
     */
    private static byte[] antiAliasedDisc(int size, float radius, float centre) {
        var samples = 8;
        var coverage = new byte[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                var covered = 0;
                for (int sy = 0; sy < samples; sy++) {
                    for (int sx = 0; sx < samples; sx++) {
                        var px = x + (sx + 0.5f) / samples - 0.5f;
                        var py = y + (sy + 0.5f) / samples - 0.5f;
                        var dx = px - centre;
                        var dy = py - centre;
                        if (Math.sqrt(dx * dx + dy * dy) <= radius) covered++;
                    }
                }
                coverage[y * size + x] = (byte) Math.round(255.0 * covered / (samples * samples));
            }
        }
        return coverage;
    }

    /**
     * Worst error within {@code band} pixels of the outline. That is the only region the shader samples when
     * it anti aliases an edge, so it is what actually decides how the glyph looks; further out the field only
     * has to be monotonic.
     */
    private static float worstErrorNearEdge(byte[] field, int fieldWidth, int padding, int size, float radius,
                                            float centre, float spread, float band) {
        var worst = 0f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                var dx = x - centre;
                var dy = y - centre;
                var expected = radius - (float) Math.sqrt(dx * dx + dy * dy);
                if (Math.abs(expected) > band) continue;
                var actual = distance(value(field, fieldWidth, x + padding, y + padding), spread);
                worst = Math.max(worst, Math.abs(expected - actual));
            }
        }
        return worst;
    }

    /**
     * The whole reason {@link DistanceTransform#fromCoverage} exists: thresholding coverage first snaps the
     * zero contour to pixel centres, and that quantization is what shows up as aliasing once the text is drawn
     * larger than the rasterization resolution.
     */
    @Test
    void subPixelTransformIsFarMoreAccurateThanThresholding() {
        var padding = 8;
        var spread = (float) padding;
        var size = 48;
        var radius = 18f;
        var centre = (size - 1) / 2f;
        var coverage = antiAliasedDisc(size, radius, centre);
        var fieldWidth = size + padding * 2;

        var thresholded = DistanceTransform.fromMask(coverage, size, size, padding, spread);
        var subPixel = DistanceTransform.fromCoverage(coverage, size, size, padding, spread);

        var band = 2f;
        var thresholdError = worstErrorNearEdge(thresholded, fieldWidth, padding, size, radius, centre, spread, band);
        var subPixelError = worstErrorNearEdge(subPixel, fieldWidth, padding, size, radius, centre, spread, band);

        // Measured: about 0.93 thresholded against about 0.52 sub-pixel on this shape. The residual is where
        // a fully covered pixel sits next to a fully empty one, which happens all round a curve: coverage says
        // nothing about how the gap between their centres is split, so half a pixel is assumed. Straight edges
        // do not have that problem, see the test below, and straight edges are what stems in text are made of.
        assertTrue(subPixelError < thresholdError * 0.6f,
                "sub-pixel transform should clearly beat thresholding near the outline, got "
                        + subPixelError + " against " + thresholdError);
    }

    /**
     * Stems, bars and the flat sides of glyphs are straight edges, and they are the parts of text where a
     * staircase in the field is most visible.
     * <p>
     * KNOWN RESIDUAL: a pixel that is fully covered but sits next to a fully empty one is seeded with half a
     * pixel, because coverage alone cannot say how the gap between their centres is split. On this edge that
     * costs a constant 0.17 pixel bias across the whole inside region. Letting such a pixel take its distance
     * from a partially covered neighbour instead removes it, but that variant measured worse on curves, so it
     * is not in yet. Thresholding is off by a full pixel here, so this is still a clear improvement.
     */
    @Test
    void subPixelTransformTracksAStraightEdge() {
        var padding = 6;
        var spread = (float) padding;
        var size = 24;
        // a vertical edge at x = 9.35, so pixel 9 is 65% covered and carries the sub-pixel information
        var edge = 9.35f;
        var coverage = new byte[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                var covered = Math.max(0f, Math.min(1f, x + 0.5f - edge));
                coverage[y * size + x] = (byte) Math.round(255 * covered);
            }
        }
        var field = DistanceTransform.fromCoverage(coverage, size, size, padding, spread);
        var fieldWidth = size + padding * 2;
        var y = padding + size / 2;
        for (int x = 4; x < 16; x++) {
            var expected = x + 0.5f - edge - 0.5f;
            if (Math.abs(expected) > 3f) continue;
            var actual = distance(value(field, fieldWidth, x + padding, y), spread);
            assertEquals(expected, actual, 0.2f, "straight edge distance at x=" + x);
        }
    }

    @Test
    void subPixelTransformKeepsTheSignCorrect() {
        var padding = 6;
        var size = 32;
        var radius = 12f;
        var centre = (size - 1) / 2f;
        var coverage = antiAliasedDisc(size, radius, centre);
        var field = DistanceTransform.fromCoverage(coverage, size, size, padding, padding);
        var fieldWidth = size + padding * 2;

        assertTrue(value(field, fieldWidth, padding + (int) centre, padding + (int) centre) > ON_EDGE,
                "the middle of the disc must read as inside");
        assertTrue(value(field, fieldWidth, 0, 0) < ON_EDGE, "the corner of the padding must read as outside");
    }

    @Test
    void subPixelTransformFallsBackForHardEdgedInput() {
        var padding = 4;
        // a hard edged square carries no sub-pixel information at all
        var size = 16;
        var coverage = new byte[size * size];
        for (int y = 4; y < 12; y++) {
            for (int x = 4; x < 12; x++) {
                coverage[y * size + x] = (byte) 0xFF;
            }
        }
        var viaCoverage = DistanceTransform.fromCoverage(coverage, size, size, padding, padding);
        var viaMask = DistanceTransform.fromMask(coverage, size, size, padding, padding);
        assertArrayEquals(viaMask, viaCoverage,
                "with no partial coverage there is nothing to recover, so it must match the threshold path");
    }

    @Test
    void blankMaskIsEntirelyOutside() {
        var padding = 2;
        var field = DistanceTransform.fromMask(new byte[8 * 8], 8, 8, padding, padding);
        for (var encoded : field) {
            assertTrue((encoded & 0xFF) < ON_EDGE, "a mask with no ink must never read as inside");
        }
    }

    /**
     * TrueType outlines arrive as anti aliased coverage from stb rather than as a binary mask, so the
     * transform has to treat half covered pixels as the edge.
     */
    @Test
    void treatsAntiAliasedCoverageAsHalfWayOnTheEdge() {
        var padding = 4;
        var width = 12;
        var height = 12;
        // a solid core with a one pixel ramp on its left edge, like a rasterized stem
        var coverage = new byte[width * height];
        for (int y = 0; y < height; y++) {
            coverage[y * width + 3] = (byte) 60;   // mostly outside
            coverage[y * width + 4] = (byte) 200;  // mostly inside
            for (int x = 5; x < width; x++) {
                coverage[y * width + x] = (byte) 0xFF;
            }
        }
        var field = DistanceTransform.fromMask(coverage, width, height, padding, padding);
        var fieldWidth = width + padding * 2;
        var y = padding + height / 2;

        assertTrue(value(field, fieldWidth, padding + 3, y) < ON_EDGE, "60/255 coverage should read as outside");
        assertTrue(value(field, fieldWidth, padding + 4, y) > ON_EDGE, "200/255 coverage should read as inside");
    }

    @Test
    void upscaleRepeatsEachPixel() {
        var mask = new byte[]{(byte) 0xFF, 0, 0, (byte) 0xFF};
        var upscaled = DistanceTransform.upscale(mask, 2, 2, 2);
        assertEquals(16, upscaled.length);
        // top left 2x2 block comes from the set pixel at (0, 0)
        assertEquals((byte) 0xFF, upscaled[0]);
        assertEquals((byte) 0xFF, upscaled[1]);
        assertEquals((byte) 0xFF, upscaled[4]);
        assertEquals((byte) 0xFF, upscaled[5]);
        // top right block comes from the clear pixel at (1, 0)
        assertEquals(0, upscaled[2]);
        assertEquals(0, upscaled[3]);
    }

    @Test
    void upscaleByOneIsIdentity() {
        var mask = new byte[]{1, 2, 3, 4};
        assertEquals(mask, DistanceTransform.upscale(mask, 2, 2, 1));
    }
}
