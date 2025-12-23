package com.lowdragmc.lowdraglib2.math.interpolate;

import com.google.common.util.concurrent.Runnables;

import java.util.function.Consumer;

/**
 * Author: KilaBash
 * Date: 2022/08/26
 */
public class Interpolator {
    private final float from;
    private final float to;
    private final float range;
    private final float duration;
    private final IEase ease;
    private final Consumer<Number> interpolate;
    private final Runnable onFinished;

    private float time = Float.NaN;
    private float startTime = 0;
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

    public boolean isFinished(){
        return finished;
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
            if (interpolate != null) {
                interpolate.accept(to);
            }
            if (onFinished != null) {
                onFinished.run();
            }
        } else {
            this.time = currentTime;
            if (interpolate != null) {
                interpolate.accept(ease.interpolate(elapsed / duration) * range + from);
            }
        }
    }
}
