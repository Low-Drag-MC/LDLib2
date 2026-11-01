package com.lowdragmc.lowdraglib2.uitest.record;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FramingTest {

    private static final float W = 960;
    private static final float H = 540;

    @Test
    void noRegionRecordsTheWholeScreen() {
        assertArrayEquals(new float[]{0, 0, W, H}, Framing.region(new TakeOptions(), null, W, H));
    }

    @Test
    void aRegionIsPaddedWithRoomBelowForTheKeys() {
        var options = new TakeOptions().padding(10);
        var region = Framing.region(options, new float[]{100, 100, 200, 80}, W, H);
        float height = 80 + 20;
        assertArrayEquals(new float[]{90, 90, 220, height + Framing.keySpace(height)}, region, 1e-4f);
    }

    @Test
    void theStripGrowsForABurnedCaptionAndGoesWithoutKeys() {
        var target = new float[]{100, 100, 200, 80};
        float height = 80 + 24;
        var withCaption = Framing.region(new TakeOptions().burnCaptions("en"), target, W, H);
        assertEquals(height + Framing.keySpace(height) + Framing.captionSpace(height), withCaption[3], 1e-4);
        var noKeys = Framing.region(new TakeOptions().keys(false), target, W, H);
        assertEquals(height, noKeys[3], 1e-4);
        var noStrip = Framing.region(new TakeOptions().keyStrip(false), target, W, H);
        assertEquals(height, noStrip[3], 1e-4);
    }

    @Test
    void aFixedSizeGrowsTheRegionToItsAspectAroundTheCentre() {
        var options = new TakeOptions().padding(0).keyStrip(false).size(1600, 900);
        var region = Framing.region(options, new float[]{400, 200, 100, 100}, W, H);
        assertEquals(16f / 9f, region[2] / region[3], 1e-4);
        assertEquals(450, region[0] + region[2] / 2, 1e-3);
        assertEquals(250, region[1] + region[3] / 2, 1e-3);
    }

    @Test
    void aRegionNeverLeavesTheScreen() {
        var options = new TakeOptions().padding(30);
        var region = Framing.region(options, new float[]{-50, 500, 300, 100}, W, H);
        assertTrue(region[0] >= 0 && region[1] >= 0);
        assertTrue(region[0] + region[2] <= W + 1e-3 && region[1] + region[3] <= H + 1e-3);
    }

    @Test
    void aRegionBiggerThanTheScreenShrinksKeepingItsAspect() {
        var fitted = Framing.fitAspect(new float[]{0, 0, 2000, 100}, 2f, W, H);
        assertEquals(2f, fitted[2] / fitted[3], 1e-4);
        assertTrue(fitted[2] <= W + 1e-3 && fitted[3] <= H + 1e-3);
    }

    @Test
    void aDegenerateRegionStillGetsAnAspect() {
        var fitted = Framing.fitAspect(new float[]{10, 10, 0, 0}, 16f / 9f, W, H);
        assertEquals(16f / 9f, fitted[2] / fitted[3], 1e-3);
    }

    @Test
    void theVideoIsAlwaysEvenSized() {
        assertArrayEquals(new int[]{1600, 900}, Framing.outputSize(new TakeOptions().size(1601, 901), new float[]{0, 0, 10, 10}, 2));
        assertArrayEquals(new int[]{300, 200}, Framing.outputSize(new TakeOptions(), new float[]{0, 0, 100.4f, 67}, 3));
        assertArrayEquals(new int[]{150, 100}, Framing.outputSize(new TakeOptions().scale(0.5f), new float[]{0, 0, 100.4f, 67}, 3));
    }

    @Test
    void aVideoSmallerThanTwoPixelsIsRefused() {
        assertNull(Framing.outputSize(new TakeOptions().scale(0.001f), new float[]{0, 0, 100, 100}, 2));
    }

    @Test
    void aZoomKeepsTheVideosAspectAndLeavesRoomBelowWhenAsked() {
        var rect = new float[]{300, 200, 100, 60};
        var plain = Framing.zoom(rect, 10, false, 16f / 9f, W, H);
        var roomy = Framing.zoom(rect, 10, true, 16f / 9f, W, H);
        assertEquals(16f / 9f, plain[2] / plain[3], 1e-4);
        assertEquals(16f / 9f, roomy[2] / roomy[3], 1e-4);
        assertTrue(roomy[3] > plain[3]);
        assertTrue(plain[0] <= 290 && plain[0] + plain[2] >= 410);
    }
}
