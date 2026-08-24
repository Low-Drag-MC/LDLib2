package com.lowdragmc.lowdraglib2.math.interpolate;

import com.google.common.util.concurrent.Runnables;
import lombok.Getter;

import java.util.function.Consumer;

/**
 * Author: KilaBash
 * Date: 2022/08/26
 */
public class Interpolator {
    public final float from;
    public final float to;
    public final float range;
    public final float duration;
    public final IEase ease;
    public final Consumer<Number> interpolate;
    public final Runnable onFinished;

    // runtime
    @Getter
    private float normalizedTime = 0;
    @Getter
    private float time = Float.NaN;
    @Getter
    private float startTime = 0;
    @Getter
    private boolean finished = false;

    public Interpolator(float from, float to, float duration, IEase ease, Consumer<Number> interpolate) {
        this(from, to, duration, ease, interpolate, Runnables.doNothing());
    }

    public Interpolator(float from, float to, float duration, IEase ease, Consumer<Number> interpolate, Runnable onFinished) {
        this.from = from;
        this.to = to;
        this.range = to - from;
        this.duration = duration;
        this.ease = ease;
        this.interpolate = interpolate;
        this.onFinished = onFinished;
    }

    public void reset() {
        time = Float.NaN;
        finished = false;
    }

    /**
     * Moves this interpolator onto a clock running at the same rate from a different origin, keeping
     * whatever progress it has already made.
     *
     * <p>The whole conversion is one offset, because that is the only thing separating two clocks both
     * derived from {@link System#nanoTime()}: an interpolator a second into a three second run stays a
     * second in, and one whose duration ran out while nobody was updating it still finishes on the very
     * next update.
     *
     * <p>Harmless before the first {@link #update}, when {@link #startTime} has not been claimed yet
     * and is about to be overwritten anyway.
     *
     * @param offset the destination clock's reading minus this one's, in seconds
     */
    public void shiftClock(float offset) {
        startTime += offset;
        if (!Float.isNaN(time)) {
            time += offset;
        }
    }

    public void update(float currentTime) {
        if (finished) {
            return;
        }

        if (Float.isNaN(this.time)) {
            startTime = currentTime;
        }

        float elapsed = currentTime - startTime;

        if (elapsed >= duration) {
            this.time = startTime + duration;
            finished = true;
            normalizedTime = 1;
            if (interpolate != null) {
                interpolate.accept(to);
            }
            if (onFinished != null) {
                onFinished.run();
            }
        } else {
            this.time = currentTime;
            normalizedTime = elapsed / duration;
            if (interpolate != null) {
                interpolate.accept(ease.interpolate(normalizedTime) * range + from);
            }
        }
    }
}
