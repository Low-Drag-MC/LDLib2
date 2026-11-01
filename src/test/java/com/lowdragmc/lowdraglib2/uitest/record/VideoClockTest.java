package com.lowdragmc.lowdraglib2.uitest.record;

import net.minecraft.util.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoClockTest {

    @AfterEach
    void stopStepping() {
        VideoClock.endStepping();
    }

    @Test
    void aTakeStepsExactlyOneFramePerAdvance() {
        VideoClock.beginStepping(60);
        long start = Util.getNanos();
        for (int i = 1; i <= 600; i++) {
            VideoClock.advanceFrame();
            assertEquals(i * 1_000_000_000L / 60, Util.getNanos() - start);
        }
        assertEquals(600, VideoClock.frames());
    }

    @Test
    void timeStandsStillBetweenFrames() throws InterruptedException {
        VideoClock.beginStepping(30);
        long before = Util.getNanos();
        Thread.sleep(20);
        assertEquals(before, Util.getNanos());
    }

    @Test
    void timeNeverJumpsWhenATakeOpensOrCloses() throws InterruptedException {
        long beforeTake = Util.getNanos();
        VideoClock.beginStepping(60);
        long opened = Util.getNanos();
        assertTrue(opened >= beforeTake);
        for (int i = 0; i < 3; i++) VideoClock.advanceFrame();
        long last = Util.getNanos();
        VideoClock.endStepping();
        long closed = Util.getNanos();
        assertTrue(closed >= last && closed - last < 50_000_000L, "jumped " + (closed - last) + "ns");
        Thread.sleep(5);
        assertTrue(Util.getNanos() > closed, "wall clock resumes after the take");
    }

    @Test
    void aTakeThatRanAheadOfRealTimeDoesNotRewindTheClock() {
        VideoClock.beginStepping(1);
        for (int i = 0; i < 100; i++) VideoClock.advanceFrame();
        long ahead = Util.getNanos();
        VideoClock.endStepping();
        assertTrue(Util.getNanos() >= ahead);
    }

    @Test
    void takesDoNotNest() {
        VideoClock.beginStepping(60);
        assertThrows(IllegalStateException.class, () -> VideoClock.beginStepping(60));
    }

    @Test
    void steppingOnlyHappensInsideATake() {
        assertFalse(VideoClock.isStepping());
        assertEquals(0, VideoClock.frames());
        VideoClock.advanceFrame();
        assertEquals(0, VideoClock.frames());
    }
}
