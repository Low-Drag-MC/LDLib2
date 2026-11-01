package com.lowdragmc.lowdraglib2.uitest.record;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorPathTest {

    private static final float SCREEN = 540;

    @Test
    void curveStartsAtTheCursorAndEndsExactlyOnTheTarget() {
        var path = CursorPath.curve(10, 20, 400, 300, 16, SCREEN, 1, new Random(1));
        var point = new float[2];
        assertFalse(path.at(0, point));
        assertEquals(10, point[0], 1e-4);
        assertEquals(20, point[1], 1e-4);
        assertTrue(path.at(path.durationSeconds(), point));
        assertEquals(400, point[0], 1e-4);
        assertEquals(300, point[1], 1e-4);
        assertTrue(path.at(path.durationSeconds() * 3, point));
        assertEquals(400, point[0], 1e-4);
    }

    @Test
    void curveNeverMovesAwayFromTheTarget() {
        var path = CursorPath.curve(0, 0, 500, 0, 16, SCREEN, 1, new Random(7));
        var point = new float[2];
        float last = Float.MAX_VALUE;
        for (int i = 0; i <= 120; i++) {
            path.at(path.durationSeconds() * i / 120f, point);
            float remaining = 500 - point[0];
            assertTrue(remaining <= last + 1e-3f, "step " + i + " went back: " + remaining + " > " + last);
            last = remaining;
        }
    }

    @Test
    void durationGrowsWithDistanceAndShrinksWithTargetSizeWithinBounds() {
        float near = CursorPath.curve(0, 0, 20, 0, 16, SCREEN, 1, new Random(3)).durationSeconds();
        float far = CursorPath.curve(0, 0, 900, 0, 16, SCREEN, 1, new Random(3)).durationSeconds();
        float farBig = CursorPath.curve(0, 0, 900, 0, 300, SCREEN, 1, new Random(3)).durationSeconds();
        assertTrue(near < far);
        assertTrue(farBig < far);
        for (float d : new float[]{1, 50, 5000}) {
            float seconds = CursorPath.curve(0, 0, d, 0, 4, SCREEN, 1, new Random(5)).durationSeconds();
            assertTrue(seconds >= 0.18f * 0.92f - 1e-4f && seconds <= 0.9f * 1.08f + 1e-4f, "duration " + seconds);
        }
    }

    @Test
    void slowdownScalesTheDuration() {
        float normal = CursorPath.curve(0, 0, 300, 100, 16, SCREEN, 1, new Random(9)).durationSeconds();
        float carried = CursorPath.curve(0, 0, 300, 100, 16, SCREEN, 1.3f, new Random(9)).durationSeconds();
        assertEquals(normal * 1.3f, carried, 1e-4);
    }

    @Test
    void aMoveOfNothingFinishesAtOnce() {
        var path = CursorPath.curve(50, 50, 50.2f, 50, 16, SCREEN, 1, new Random(1));
        var point = new float[2];
        assertEquals(0, path.durationSeconds());
        assertTrue(path.at(0, point));
        assertEquals(50.2f, point[0], 1e-4);
    }

    @Test
    void elbowGoesAcrossBeforeItGoesDown() {
        var path = CursorPath.elbow(100, 100, 300, 260, 12, SCREEN, new Random(2));
        var point = new float[2];
        boolean leftTheRow = false;
        for (int i = 0; i <= 200; i++) {
            path.at(path.durationSeconds() * i / 200f, point);
            if (point[1] > 100 + 1e-3f) {
                leftTheRow = true;
                assertEquals(300, point[0], 1e-3, "moved down before reaching the target's column");
            } else {
                assertFalse(leftTheRow, "came back up to the starting row");
            }
        }
        assertEquals(260, point[1], 1e-4);
    }

    @Test
    void theSameSeedPlansTheSamePath() {
        var a = CursorPath.curve(0, 0, 640, 360, 20, SCREEN, 1, new Random(42));
        var b = CursorPath.curve(0, 0, 640, 360, 20, SCREEN, 1, new Random(42));
        var pa = new float[2];
        var pb = new float[2];
        assertEquals(a.durationSeconds(), b.durationSeconds());
        for (int i = 0; i <= 10; i++) {
            a.at(a.durationSeconds() * i / 10f, pa);
            b.at(b.durationSeconds() * i / 10f, pb);
            assertEquals(pa[0], pb[0]);
            assertEquals(pa[1], pb[1]);
        }
    }
}
