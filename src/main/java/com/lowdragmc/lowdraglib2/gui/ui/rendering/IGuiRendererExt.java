package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererPool;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/// Duck-type interface implemented on {@code GuiRenderer} via mixin
/// to expose sub-state rendering hooks for visual layers.
public interface IGuiRendererExt {
    /// Swap the renderer's internal {@code GuiRenderState}. Used by visual layer
    /// PIP renderer to install a captured sub-state before invoking render().
    void ldlib2$setRenderState(GuiRenderState state);

    MultiBufferSource.BufferSource ldlib2$getBufferSource();

    /// The picture-in-picture renderers this gui renderer can dispatch, keyed by state class.
    ///
    /// Exposed so a sub-renderer can inherit them instead of declaring its own. The set is not
    /// static: vanilla registers entities, skins, signs and the rest, and every mod adds its own
    /// through `RegisterPictureInPictureRenderersEvent` — LDLib2 itself registers the world scene
    /// there. A sub-renderer built with a hand-written list silently draws nothing for every kind it
    /// forgot, which is how a scene inside a floating window came out blank while its chrome drew
    /// fine.
    Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> ldlib2$getPictureInPictureRendererPools();

    /// Adopt another renderer's pools wholesale. Replaces rather than merges: the map a
    /// `GuiRenderer` builds from its registration list is immutable.
    void ldlib2$setPictureInPictureRendererPools(
            Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> pools);
    SubmitNodeCollector ldlib2$getSubmitNodeCollector();
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

    /// The destination metrics {@code draw()} resolves against the game window when left alone.
    ///
    /// Two things are derived from those metrics and both are wrong for a UI hosted in its own OS
    /// window, which is laid out and rendered at that window's size:
    ///
    ///  - the orthographic projection, built from the window's gui-space extent. With the game
    ///    window's, contents come out scaled and offset by the ratio between the two.
    ///  - the scissor box, which {@code enableScissor} flips against the game window's *pixel*
    ///    height. A clipped element in a smaller window ends up with a box entirely outside the
    ///    target and is simply not drawn — which is why a scene view came up empty while the
    ///    unclipped chrome around it drew perfectly.
    ///
    /// A visual layer needs neither: its off-target is allocated at the game window's size, so
    /// vanilla's numbers already are the right ones.
    ///
    /// Paired with {@link #ldlib2$pushTargetOverride}: the target says where pixels land, this says
    /// in what coordinate space and within what bounds.
    record OrthoExtent(float width, float height, int framebufferHeight, int guiScale) {}

    static void ldlib2$pushOrthoOverride(float guiWidth, float guiHeight, int framebufferHeight, int guiScale) {
        State.ORTHO_STACK.push(new OrthoExtent(guiWidth, guiHeight, framebufferHeight, guiScale));
    }

    static void ldlib2$popOrthoOverride() {
        State.ORTHO_STACK.pop();
    }

    @Nullable
    static OrthoExtent ldlib2$peekOrthoOverride() {
        return State.ORTHO_STACK.peek();
    }

    /// Stored by the GuiRendererMixin's @Inject on render(); read by sub-renderers
    /// that need a no-fog GpuBufferSlice without accessing GameRenderer.fogRenderer.
    static void ldlib2$setLastFogBuffer(GpuBufferSlice buffer) {
        State.LAST_FOG_BUFFER = buffer;
    }

    @Nullable
    static GpuBufferSlice ldlib2$getLastFogBuffer() {
        return State.LAST_FOG_BUFFER;
    }

    /// Mixin classes cannot hold non-private static members, so global state for
    /// {@code IGuiRendererExt} lives here instead.
    final class State {
        static final java.util.Deque<RenderTarget> TARGET_STACK = new java.util.ArrayDeque<>();
        static final java.util.Deque<OrthoExtent> ORTHO_STACK = new java.util.ArrayDeque<>();
        @Nullable static GpuBufferSlice LAST_FOG_BUFFER;
        private State() {}
    }
}
