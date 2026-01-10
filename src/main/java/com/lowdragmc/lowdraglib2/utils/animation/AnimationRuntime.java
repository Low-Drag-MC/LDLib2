package com.lowdragmc.lowdraglib2.utils.animation;

import com.lowdragmc.lowdraglib2.math.interpolate.Interpolator;
import lombok.Getter;

public class AnimationRuntime {
    public final KeyFrameAnimation animation;
    // runtime
    private final float initialTime;
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
}
