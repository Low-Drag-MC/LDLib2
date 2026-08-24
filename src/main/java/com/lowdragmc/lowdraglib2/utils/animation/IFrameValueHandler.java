package com.lowdragmc.lowdraglib2.utils.animation;

import org.jetbrains.annotations.Nullable;

public interface IFrameValueHandler<T> {
    void accept(AnimationRuntime runtime, T t);

    void onFinished(AnimationRuntime runtime);

    /**
     * What this handler animates, when that is something the engine's client should be able to
     * recognise — a {@code UIElement}, for everything in the UI framework.
     *
     * <p>Only ever compared by identity, and only by {@link AnimationEngine#handOver}, which needs it
     * to tell which running animations belong to a subtree that has moved to another engine.
     *
     * @return the animated object, or {@code null} for a handler driving something anonymous
     */
    @Nullable
    default Object owner() {
        return null;
    }
}
