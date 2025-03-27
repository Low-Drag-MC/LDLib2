package com.lowdragmc.lowdraglib.math.interpolate;

import java.util.function.Consumer;

/**
 * Author: KilaBash
 * Date: 2022/08/26
 */
public class Interpolator {
    private final float from;
    private final float to;
    private final float durationTick;
    private final IEase ease;
    private final Consumer<Number> interpolate;
    private final Consumer<Number> callback;

    private float tick = -1;
    private float startTick = 0;

    public Interpolator(float from, float to, float durationTick, IEase ease, Consumer<Number> interpolate) {
        this(from, to, durationTick, ease, interpolate, null);
    }

    public Interpolator(float from, float to, float durationTick, IEase ease, Consumer<Number> interpolate, Consumer<Number> callback) {
        this.from = from;
        this.to = to;
        this.durationTick = durationTick;
        this.ease = ease;
        this.interpolate = interpolate;
        this.callback = callback;
    }

    public void reset() {
        tick = -1;
    }

    public boolean isFinished(){
        return tick == durationTick;
    }

    public void update(float tickTime) {
        if (tick == -2) {
            return;
        }

        if (tick == -1) {
            startTick = tickTime;
        }

        if ((tick - startTick) >= durationTick) {
            tick = -2;
            if (interpolate != null) {
                interpolate.accept(ease.getInterpolation(1) * (to - from) + from);
            }
            if (callback != null) {
                callback.accept(ease.getInterpolation(1) * (to - from) + from);
            }
        } else {
            tick = tickTime;
            if (interpolate != null) {
                interpolate.accept(ease.getInterpolation((tick - startTick) / durationTick) * (to - from) + from);
            }
        }
    }
}
