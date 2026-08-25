package com.lowdragmc.lowdraglib2.utils.animation;

import com.lowdragmc.lowdraglib2.gui.ui.style.IValueInterpolator;
import com.lowdragmc.lowdraglib2.math.interpolate.IEase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers moving a running animation from one {@link AnimationEngine} to another, which is what keeps it
 * alive when the element tree driving it is re-hosted by a different UI.
 *
 * <p>The clock arithmetic is exercised through {@link AnimationRuntime} directly, where the time of
 * every update is an argument. The engine's own clock is real, so its tests only assert what does not
 * depend on how much wall time a test run happens to take.
 */
class AnimationHandOverTest {

    private static final float DURATION = 10f;
    private static final IEase LINEAR = t -> t;
    private static final IValueInterpolator<Float> LERP =
            (from, to, fraction) -> from + (to - from) * fraction;

    /**
     * Records the last value it was handed, so a test can see where an animation had got to.
     */
    private static final class Recorder implements IFrameValueHandler<Float> {
        private final Object owner;
        private float last = Float.NaN;
        private boolean finished;

        private Recorder(Object owner) {
            this.owner = owner;
        }

        @Override
        public Object owner() {
            return owner;
        }

        @Override
        public void accept(AnimationRuntime runtime, Float value) {
            last = value;
        }

        @Override
        public void onFinished(AnimationRuntime runtime) {
            finished = true;
        }
    }

    /**
     * An animation running {@code 0 -> 100} over {@link #DURATION}, so a value reads directly as the
     * percentage of the way through it is.
     */
    private static KeyFrameAnimation animationOf(Recorder recorder) {
        return KeyFrameAnimation.of(new Animation(DURATION, 0f, LINEAR),
                new KFExecutor<>(KeyFrames.of(LERP, 0f, 100f), recorder));
    }

    @Test
    void shiftedClockKeepsTheProgressAlreadyMade() {
        var recorder = new Recorder("owner");
        var runtime = new AnimationRuntime(0f, animationOf(recorder));

        runtime.update(0f);
        runtime.update(3f);
        assertEquals(30f, recorder.last, 1e-3);

        // Same instant, read off a clock whose origin is 1000 seconds earlier.
        runtime.shiftClock(1000f);
        runtime.update(1003f);
        assertEquals(30f, recorder.last, 1e-3);

        runtime.update(1008f);
        assertEquals(80f, recorder.last, 1e-3);
        assertFalse(runtime.isFinished());
    }

    @Test
    void animationWhoseDurationRanOutWhileUnattendedFinishesOnTheNextUpdate() {
        var recorder = new Recorder("owner");
        var runtime = new AnimationRuntime(0f, animationOf(recorder));

        runtime.update(0f);
        runtime.update(1f);
        assertEquals(10f, recorder.last, 1e-3);

        // Handed to another engine after a long spell during which nobody drew either of them.
        runtime.shiftClock(1000f);
        runtime.update(1060f);

        assertTrue(runtime.isFinished());
        assertTrue(recorder.finished);
        assertEquals(100f, recorder.last, 1e-3);
    }

    @Test
    void delayIsCarriedAcrossTheShiftRatherThanRestarted() {
        var recorder = new Recorder("owner");
        var delayed = KeyFrameAnimation.of(new Animation(DURATION, 5f, LINEAR),
                new KFExecutor<>(KeyFrames.of(LERP, 0f, 100f), recorder));
        var runtime = new AnimationRuntime(0f, delayed);

        runtime.update(2f);
        assertTrue(Float.isNaN(recorder.last), "still inside the delay");

        runtime.shiftClock(1000f);
        runtime.update(1004f);
        assertTrue(Float.isNaN(recorder.last), "the delay moved with the clock, it did not restart");

        runtime.update(1006f);
        assertEquals(0f, recorder.last, 1e-3);
    }

    @Test
    void handOverMovesARunningAnimationAndLeavesTheRestBehind() {
        var source = new AnimationEngine();
        var destination = new AnimationEngine();
        var moving = new Recorder("moving");
        var staying = new Recorder("staying");
        source.play(animationOf(moving));
        source.play(animationOf(staying));
        source.updateFrame();

        source.handOver(destination, owner -> owner == moving.owner);

        // Only the one that stayed behind still advances on the source.
        staying.last = Float.NaN;
        moving.last = Float.NaN;
        source.updateFrame();
        assertFalse(Float.isNaN(staying.last));
        assertTrue(Float.isNaN(moving.last), "the moved animation must no longer run on the source");

        staying.last = Float.NaN;
        destination.updateFrame();
        assertFalse(Float.isNaN(moving.last), "the moved animation must run on the destination");
        assertTrue(Float.isNaN(staying.last), "nothing else may be dragged along");
    }

    @Test
    void handOverMovesAnAnimationThatHasNotHadItsFirstFrameYet() {
        var source = new AnimationEngine();
        var destination = new AnimationEngine();
        var recorder = new Recorder("owner");
        source.play(animationOf(recorder));

        source.handOver(destination, owner -> owner == recorder.owner);

        source.updateFrame();
        assertTrue(Float.isNaN(recorder.last), "the source never started it");

        destination.updateFrame();
        assertEquals(0f, recorder.last, 1e-3);
    }

    @Test
    void cancellingAfterAHandOverStillStopsTheAnimation() {
        var source = new AnimationEngine();
        var destination = new AnimationEngine();
        var recorder = new Recorder("owner");
        var subscription = source.play(animationOf(recorder));
        source.updateFrame();

        source.handOver(destination, owner -> owner == recorder.owner);
        subscription.unsubscribe();

        recorder.last = Float.NaN;
        destination.updateFrame();
        assertTrue(Float.isNaN(recorder.last), "unsubscribe has to reach the engine now holding it");
    }

    @Test
    void handOverToTheSameEngineChangesNothing() {
        var engine = new AnimationEngine();
        var recorder = new Recorder("owner");
        engine.play(animationOf(recorder));
        engine.updateFrame();

        engine.handOver(engine, owner -> true);

        recorder.last = Float.NaN;
        engine.updateFrame();
        assertFalse(Float.isNaN(recorder.last));
    }
}
