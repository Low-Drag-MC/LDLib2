package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.utils.Scope;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL30;

/**
 * Remembers where drawing was going, so a render pass can put it back.
 *
 * <p>The rule this exists to enforce: a pass that binds its own render target must restore
 * <em>whatever was bound before</em>, never {@code Minecraft.getMainRenderTarget()}. Those two are
 * the same thing only while the game window is the only place a frame can land, and that assumption
 * is wrong in two situations that both already exist:
 *
 * <ul>
 *   <li>a UI drawn into an off-screen target — an element with {@code overflow: hidden} or an opacity
 *       below one renders through {@link com.lowdragmc.lowdraglib2.gui.ui.rendering.UIVisualLayer},
 *       and a scene or effect drawn inside it would escape into the game's frame;</li>
 *   <li>a UI hosted in its own operating-system window, where the main target is a different window
 *       entirely and anything restored to it is simply drawn somewhere the user is not looking.</li>
 * </ul>
 *
 * <p>Usage is a try-with-resources around the pass:
 *
 * <pre>{@code
 * try (var ignored = RenderTargetScope.capture()) {
 *     myTarget.bindWrite(true);
 *     ...
 * } // previous framebuffer and viewport are back
 * }</pre>
 *
 * <p>Both values come from {@code GlStateManager}'s own mirrors rather than {@code glGetIntegerv},
 * so capturing costs nothing and never stalls the pipeline.
 */
@OnlyIn(Dist.CLIENT)
public final class RenderTargetScope implements Scope {

    private final int framebuffer;
    private final int viewportX;
    private final int viewportY;
    private final int viewportWidth;
    private final int viewportHeight;

    private RenderTargetScope(int framebuffer, int x, int y, int width, int height) {
        this.framebuffer = framebuffer;
        this.viewportX = x;
        this.viewportY = y;
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    /**
     * Captures the bound draw framebuffer and the current viewport.
     */
    public static RenderTargetScope capture() {
        RenderSystem.assertOnRenderThread();
        return new RenderTargetScope(currentFramebuffer(),
                GlStateManager.Viewport.x(), GlStateManager.Viewport.y(),
                GlStateManager.Viewport.width(), GlStateManager.Viewport.height());
    }

    /**
     * The framebuffer object currently bound for drawing; {@code 0} is the window's own.
     */
    public static int currentFramebuffer() {
        return GlStateManager.getBoundFramebuffer();
    }

    @Override
    public void close() {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        // Through RenderSystem rather than glViewport directly, so GlStateManager's mirror of the
        // viewport matches the driver again — otherwise the next state-deduplicating call is wrong.
        RenderSystem.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
    }
}
