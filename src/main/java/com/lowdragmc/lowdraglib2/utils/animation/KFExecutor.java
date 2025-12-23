package com.lowdragmc.lowdraglib2.utils.animation;

public record KFExecutor<T>(KeyFrames<T> keyFrames, IFrameValueHandler<T> handler) {
    public void apply(AnimationRuntime runtime, float lerp) {
        var value = keyFrames.getValue(lerp);
        handler.accept(runtime, value);
    }

    public void onFinished(AnimationRuntime runtime) {
        handler.onFinished(runtime);
    }
}
