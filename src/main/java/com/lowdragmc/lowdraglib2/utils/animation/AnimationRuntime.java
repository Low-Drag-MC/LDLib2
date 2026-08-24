package com.lowdragmc.lowdraglib2.utils.animation;

import com.lowdragmc.lowdraglib2.math.interpolate.Interpolator;
import lombok.Getter;

public class AnimationRuntime {
    public final KeyFrameAnimation animation;
    // runtime
    private float initialTime;
    @Getter
    private final Interpolator interpolator;

    public AnimationRuntime(float initialTime, KeyFrameAnimation animation) {
        this.initialTime = initialTime;
        this.animation = animation;
        this.interpolator = new Interpolator(0, 1, animation.animation().duration(), animation.animation().ease(),
                this::onInterpolate, this::onFinished);
    }

    private void onFinished() {
        for (var executor : animation.kfExecutors()) {
            executor.onFinished(this);
        }
    }

    private void onInterpolate(Number number) {
        var lerp = number.floatValue();
        for (var executor : animation.kfExecutors()) {
            executor.apply(this, lerp);
        }
    }

    public void update(float currentTime){
        if (animation.animation().delay() > currentTime - initialTime) return;
        this.interpolator.update(currentTime);
    }

    public boolean isFinished(){
        return interpolator.isFinished();
    }

    /**
     * Moves this runtime, delay included, onto the clock of another {@link AnimationEngine}.
     *
     * @param offset the destination engine's reading minus the current one's, in seconds
     * @see Interpolator#shiftClock(float)
     */
    public void shiftClock(float offset) {
        this.initialTime += offset;
        this.interpolator.shiftClock(offset);
    }
}
