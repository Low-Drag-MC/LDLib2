package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.core.mixins.accessor.GameRendererAccessor;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.PictureInPictureRendererPoolAccessor;
import com.lowdragmc.lowdraglib2.utils.Scope;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererPool;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;

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

    /// The picture-in-picture renderers this gui renderer can dispatch, keyed by state class.
    ///
    /// Exposed so a sub-renderer can inherit them instead of declaring its own. The set is not
    /// static: vanilla registers entities, skins, signs and the rest, and every mod adds its own
    /// through `RegisterPictureInPictureRenderersEvent` — LDLib2 itself registers the world scene
    /// there. A sub-renderer built with a hand-written list silently draws nothing for every kind it
    /// forgot, which is how a scene inside a floating window came out blank while its chrome drew
    /// fine.
    Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> ldlib2$getPictureInPictureRendererPools();

    FeatureRenderDispatcher ldlib2$getFeatureRenderDispatcher();

    /// A {@code GuiRenderer} that flushes its own {@code GuiRenderState} into somewhere other than
    /// the game's frame, dispatching every picture-in-picture kind the game renderer can.
    ///
    /// The registrations are inherited rather than declared for the reason spelled out on
    /// {@link #ldlib2$getPictureInPictureRendererPools()}, and the pools are freshly built from them
    /// rather than shared: a pool keys its reuse bookkeeping on "the renderers used this frame" and
    /// assumes one gui renderer per frame. With two, this renderer's states can be handed a renderer
    /// the game's has already drawn into, and the blit then samples someone else's picture.
    ///
    /// The feature dispatcher is borrowed rather than duplicated: it is per-frame scratch, and every
    /// pass built on this runs to completion inside one frame on the render thread, so nothing is in
    /// flight to clash with.
    ///
    /// Caller owns the result and must {@code close()} it — a {@code GuiRenderer} holds an off-heap
    /// vertex buffer, an item atlas and a render target per pooled picture-in-picture renderer, none
    /// of which the garbage collector can reclaim.
    static GuiRenderer ldlib2$createSubRenderer(GuiRenderState state) {
        var main = ((GameRendererAccessor) (Object) Minecraft.getInstance().gameRenderer).ldlib2$getGuiRenderer();
        var mainExt = (IGuiRendererExt) (Object) main;
        var registrations = new ArrayList<PictureInPictureRendererRegistration<?>>();
        for (var pool : mainExt.ldlib2$getPictureInPictureRendererPools().values()) {
            registrations.add(((PictureInPictureRendererPoolAccessor) pool).ldlib2$getFactory());
        }
        return new GuiRenderer(state, mainExt.ldlib2$getFeatureRenderDispatcher(), registrations);
    }

    /// Override the {@code RenderTarget} that render() will write to, until the scope is closed.
    static Scope ldlib2$targetOverride(RenderTarget target) {
        State.TARGET_STACK.push(target);
        return State.TARGET_STACK::pop;
    }

    @Nullable
    static RenderTarget ldlib2$peekTargetOverride() {
        return State.TARGET_STACK.peek();
    }

    /// The window metrics {@code GuiRenderer} resolves everything size-dependent against.
    ///
    /// In 26.2 that is a single object — `gameRenderState().windowRenderState` — read in four
    /// places, and every one of them is wrong for a UI hosted in its own OS window, which is laid
    /// out and rendered at that window's size:
    ///
    ///  - the orthographic projection in `draw()`, built from `width / guiScale` by
    ///    `height / guiScale`. With the game window's numbers contents come out scaled and offset by
    ///    the ratio between the two.
    ///  - the scissor box, which `enableScissor` clamps against the window's *pixel* extent and
    ///    flips against its pixel height. A clipped element in a smaller window ends up with a box
    ///    entirely outside the target and is simply not drawn — which is why a scene view came up
    ///    empty while the unclipped chrome around it drew perfectly.
    ///  - the gui scale handed to picture-in-picture renderers and to the item atlas, which decides
    ///    the resolution their off-screen textures are rendered at.
    ///
    /// Overriding the state itself rather than patching each call site keeps the four consistent —
    /// a surface with a scale of its own would otherwise lay out at one scale and rasterise items at
    /// another.
    ///
    /// A visual layer needs none of this: its off-target is allocated at the game window's size, so
    /// vanilla's numbers already are the right ones.
    ///
    /// Only the three fields above are set. {@code appropriateLineWidth} and {@code isMinimized} are
    /// left at their defaults deliberately: no reader reached through this redirect touches them, and
    /// inventing values would be guessing rather than overriding.
    ///
    /// Carries no gui-space extent, unlike 26.1's ortho override, because it does not have to: 26.2
    /// derives the projection from these same fields as `width / guiScale`, exactly, so a gui unit
    /// is exactly {@code guiScale} pixels here and on the game window alike. That is what lets the
    /// precise scissor quantise against one scale factor instead of a ratio it has to reconstruct.
    ///
    /// Paired with {@link #ldlib2$targetOverride}: the target says where pixels land, this says in
    /// what coordinate space and within what bounds. Both come off the same {@code RenderTarget}, so
    /// they cannot disagree about the size.
    static Scope ldlib2$windowOverride(RenderTarget target, int guiScale) {
        var state = new WindowRenderState();
        state.width = target.width;
        state.height = target.height;
        state.guiScale = guiScale;
        State.WINDOW_STACK.push(state);
        return State.WINDOW_STACK::pop;
    }

    @Nullable
    static WindowRenderState ldlib2$peekWindowOverride() {
        return State.WINDOW_STACK.peek();
    }

    /// Mixin classes cannot hold non-private static members, so global state for
    /// {@code IGuiRendererExt} lives here instead.
    final class State {
        static final java.util.Deque<RenderTarget> TARGET_STACK = new java.util.ArrayDeque<>();
        static final java.util.Deque<WindowRenderState> WINDOW_STACK = new java.util.ArrayDeque<>();
        private State() {}
    }
}
