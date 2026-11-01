package com.lowdragmc.lowdraglib2.uitest.record;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacingTest {

    @Test
    void theSameSeedGivesTheSameRhythm() {
        var a = new Pacing(17);
        var b = new Pacing(17);
        for (int i = 0; i < 50; i++) {
            assertEquals(a.beforePress(), b.beforePress());
            assertEquals(a.beforeChar('x'), b.beforeChar('x'));
        }
    }

    @Test
    void everyBeatStaysInItsRange() {
        var pacing = new Pacing(3);
        for (int i = 0; i < 500; i++) {
            assertBetween(pacing.beforePress(), 90, 150);
            assertBetween(pacing.pressHold(), 70, 105);
            assertBetween(pacing.afterRelease(), 110, 170);
            assertBetween(pacing.beforeKey(), 80, 140);
            assertBetween(pacing.scrollGap(), 80, 120);
        }
    }

    @Test
    void aDoubleClickFitsInsideTheThreeHundredMillisecondWindow() {
        var pacing = new Pacing(11);
        for (int i = 0; i < 500; i++) {
            // press, held, released, the next press: what ModularUI measures between the two downs
            long downToDown = pacing.quickHold() + pacing.doubleClickGap();
            assertTrue(downToDown < 200, "only " + (300 - downToDown) + "ms left for frames and settle");
        }
    }

    @Test
    void typingPausesAfterSpacesAndPunctuation() {
        var pacing = new Pacing(5);
        long plain = 0, afterSpace = 0, afterStop = 0;
        for (int i = 0; i < 300; i++) {
            plain += pacing.beforeChar('a');
            afterSpace += pacing.beforeChar(' ');
            afterStop += pacing.beforeChar('.');
        }
        assertTrue(afterSpace > plain);
        assertTrue(afterStop > afterSpace);
    }

    private static void assertBetween(long value, long min, long max) {
        assertTrue(value >= min && value <= max, value + " not in [" + min + ", " + max + "]");
    }
}
