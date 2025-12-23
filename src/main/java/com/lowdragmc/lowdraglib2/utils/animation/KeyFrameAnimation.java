package com.lowdragmc.lowdraglib2.utils.animation;

public record KeyFrameAnimation(KFExecutor<?>[] kfExecutors, Animation animation) {
    public static KeyFrameAnimation of (Animation animation, KFExecutor<?>... kfExecutors) {
        return new KeyFrameAnimation(kfExecutors, animation);
    }
}
