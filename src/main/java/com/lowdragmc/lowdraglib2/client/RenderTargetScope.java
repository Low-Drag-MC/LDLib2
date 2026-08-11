package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.utils.Scope;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;

/**
 * Remembers where drawing was going, so a render pass can put it back.
 *
 * <p>The rule this exists to enforce: a pass that redirects rendering into its own textures must
 * restore <em>whatever was there before</em>, never {@code Minecraft.getMainRenderTarget()}. Those
 * two are the same thing only while the game window is the only place a frame can land, and that
 * assumption is wrong in two situations that both already exist:
 *
 * <ul>
 *   <li>a UI drawn into an off-screen target — an element with a mask or an opacity below one renders
 *       through the visual-layer picture-in-picture pass, and a scene drawn inside it would escape
 *       into the game's frame;</li>
 *   <li>a UI hosted in its own operating-system window, where the main target is a different window
 *       entirely and anything restored to it is simply drawn somewhere the user is not looking.</li>
 * </ul>
 *
 * <p>In 26.1 "where drawing goes" is not a bound framebuffer object but the pair of output texture
 * overrides on {@link RenderSystem}, which the command encoder resolves when it opens a render pass.
 * Capturing is therefore two field reads.
 *
 * <p>Usage is a try-with-resources around the pass:
 *
 * <pre>{@code
 * try (var ignored = RenderTargetScope.redirect(colorView, depthView)) {
 *     ...
 * } // previous overrides and viewport are back
 * }</pre>
 */
public final class RenderTargetScope implements Scope {

    @Nullable
    private final GpuTextureView color;
    @Nullable
    private final GpuTextureView depth;

    private RenderTargetScope(@Nullable GpuTextureView color, @Nullable GpuTextureView depth) {
        this.color = color;
        this.depth = depth;
    }

    /**
     * Captures the current output overrides without changing them.
     */
    public static RenderTargetScope capture() {
        RenderSystem.assertOnRenderThread();
        return new RenderTargetScope(RenderSystem.outputColorTextureOverride, RenderSystem.outputDepthTextureOverride);
    }

    /**
     * Captures the current output overrides and redirects drawing into {@code color} / {@code depth}
     * until the returned scope is closed.
     */
    public static RenderTargetScope redirect(@Nullable GpuTextureView color, @Nullable GpuTextureView depth) {
        var scope = capture();
        RenderSystem.outputColorTextureOverride = color;
        RenderSystem.outputDepthTextureOverride = depth;
        return scope;
    }

    @Override
    public void close() {
        RenderSystem.outputColorTextureOverride = color;
        RenderSystem.outputDepthTextureOverride = depth;
        // Deliberately no glViewport here. In 26.1 the viewport is owned per render pass —
        // GlCommandEncoder sets it from the bound colour texture every time it opens one — so there
        // is nothing to restore, and writing one leaks whatever size this pass happened to use into
        // every draw that follows. That is what left the whole UI rendered into a small rectangle in
        // the bottom-left corner of the window, the GL origin being where a too-small viewport puts
        // it.
    }
}
