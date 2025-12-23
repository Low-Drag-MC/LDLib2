package com.lowdragmc.lowdraglib2.utils.animation;

public interface IFrameValueHandler<T> {
    void accept(AnimationRuntime runtime, T t);

    void onFinished(AnimationRuntime runtime);
}
