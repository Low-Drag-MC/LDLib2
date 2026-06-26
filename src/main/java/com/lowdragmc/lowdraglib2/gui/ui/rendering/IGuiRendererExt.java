package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.jspecify.annotations.Nullable;

/// Duck-type interface implemented on {@code GuiRenderer} via mixin
/// to expose sub-state rendering hooks for visual layers.
///
/// 26.2: GuiRenderer no longer exposes a MultiBufferSource.BufferSource / SubmitNodeCollector, and
/// its `render()` is no-arg (fog is handled internally), so those accessors and the fog-buffer
/// plumbing were removed.
public interface IGuiRendererExt {
    /// Swap the renderer's internal {@code GuiRenderState}. Used by visual layer
    /// PIP renderer to install a captured sub-state before invoking render().
    void ldlib2$setRenderState(GuiRenderState state);

    FeatureRenderDispatcher ldlib2$getFeatureRenderDispatcher();

    /// Override the {@code RenderTarget} that the next render() call will write to.
    /// Pass null to clear. ThreadLocal-scoped — caller must pair push/pop.
    static void ldlib2$pushTargetOverride(RenderTarget target) {
        State.TARGET_STACK.push(target);
    }

    static void ldlib2$popTargetOverride() {
        State.TARGET_STACK.pop();
    }

    @Nullable
    static RenderTarget ldlib2$peekTargetOverride() {
        return State.TARGET_STACK.peek();
    }

    /// Mixin classes cannot hold non-private static members, so global state for
    /// {@code IGuiRendererExt} lives here instead.
    final class State {
        static final java.util.Deque<RenderTarget> TARGET_STACK = new java.util.ArrayDeque<>();
        private State() {}
    }
}
