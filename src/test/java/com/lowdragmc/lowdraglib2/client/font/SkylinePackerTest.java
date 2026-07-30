package com.lowdragmc.lowdraglib2.client.font;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkylinePackerTest {

    private record Rect(int x, int y, int width, int height) {
        boolean overlaps(Rect other) {
            return x < other.x + other.width && other.x < x + width
                    && y < other.y + other.height && other.y < y + height;
        }
    }

    @Test
    void rejectsRectanglesLargerThanThePage() {
        var packer = new SkylinePacker(64, 64);
        var out = new int[2];
        assertFalse(packer.insert(65, 10, out));
        assertFalse(packer.insert(10, 65, out));
        assertFalse(packer.insert(0, 10, out));
        assertFalse(packer.insert(10, 0, out));
    }

    @Test
    void fillsAPageExactlyWithAUniformGrid() {
        var packer = new SkylinePacker(64, 64);
        var out = new int[2];
        for (int i = 0; i < 64; i++) {
            assertTrue(packer.insert(8, 8, out), "expected the " + i + "th 8x8 cell to fit in a 64x64 page");
        }
        assertFalse(packer.insert(8, 8, out), "the page should be full after 64 cells");
    }

    @Test
    void placedRectanglesNeverOverlapOrLeaveThePage() {
        var random = new Random(20260729L);
        var packer = new SkylinePacker(256, 256);
        var placed = new ArrayList<Rect>();
        var out = new int[2];

        for (int i = 0; i < 500; i++) {
            var width = 1 + random.nextInt(40);
            var height = 1 + random.nextInt(40);
            if (!packer.insert(width, height, out)) {
                continue;
            }
            var rect = new Rect(out[0], out[1], width, height);
            assertTrue(rect.x() >= 0 && rect.y() >= 0
                            && rect.x() + rect.width() <= 256 && rect.y() + rect.height() <= 256,
                    "rectangle left the page: " + rect);
            for (var other : placed) {
                assertFalse(rect.overlaps(other), rect + " overlaps " + other);
            }
            placed.add(rect);
        }

        assertFalse(placed.isEmpty(), "expected at least some rectangles to be placed");
    }

    @Test
    void reportsFailureInsteadOfOverflowingWhenFull() {
        var packer = new SkylinePacker(32, 32);
        var out = new int[2];
        var accepted = new ArrayList<Rect>();
        // keep inserting until it refuses, then confirm it keeps refusing rather than producing bad slots
        for (int i = 0; i < 200; i++) {
            if (packer.insert(7, 7, out)) {
                accepted.add(new Rect(out[0], out[1], 7, 7));
            }
        }
        assertFalse(packer.insert(7, 7, out));
        assertTotalAreaFits(accepted, 32 * 32);
    }

    private void assertTotalAreaFits(List<Rect> rects, int pageArea) {
        var area = rects.stream().mapToInt(r -> r.width() * r.height()).sum();
        assertTrue(area <= pageArea, "packed area " + area + " exceeds the page area " + pageArea);
    }
}
