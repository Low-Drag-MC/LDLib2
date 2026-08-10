package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * A UI destination that is not the game window: a render target of its own, presented somewhere else.
 *
 * <p>The gui scale is deliberately <em>not</em> independent — it reads the game window's. Font
 * rasterisation picks its glyph bucket from {@code Minecraft.getWindow().getGuiScale()} deep inside
 * the text engine, with no way to pass a different one down, so a surface with its own scale would
 * render text at the wrong weight and spacing. Sharing the scale keeps every existing pixel
 * calculation valid; only the framebuffer size differs.
 */
@OnlyIn(Dist.CLIENT)
public final class OffscreenSurface implements UISurface {

    @Nullable
    private TextureTarget target;
    private long windowHandle;
    private int screenWidth;
    private int screenHeight;

    /**
     * @param windowHandle the GLFW window this will be presented in, for cursor and key queries
     * @param screenWidth  window size in screen coordinates, which is not the framebuffer size on a
     *                     HiDPI display
     */
    public OffscreenSurface(long windowHandle, int framebufferWidth, int framebufferHeight,
                            int screenWidth, int screenHeight) {
        this.windowHandle = windowHandle;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        resize(framebufferWidth, framebufferHeight, screenWidth, screenHeight);
    }

    /**
     * Grows or shrinks the target. Cheap when nothing changed; a real reallocation otherwise, since
     * {@code RenderTarget#resize} destroys and recreates the attachments.
     */
    public void resize(int framebufferWidth, int framebufferHeight, int screenWidth, int screenHeight) {
        this.screenWidth = Math.max(1, screenWidth);
        this.screenHeight = Math.max(1, screenHeight);
        var width = Math.max(1, framebufferWidth);
        var height = Math.max(1, framebufferHeight);
        if (target == null) {
            target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 1);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
        }
    }

    @Override
    public RenderTarget target() {
        if (target == null) {
            throw new IllegalStateException("Surface has been destroyed");
        }
        return target;
    }

    /**
     * The colour attachment to blit from, or {@code -1} once destroyed.
     */
    public int colorTextureId() {
        return target == null ? -1 : target.getColorTextureId();
    }

    @Override
    public double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    @Override
    public int screenWidth() {
        return screenWidth;
    }

    @Override
    public int screenHeight() {
        return screenHeight;
    }

    @Override
    public long windowHandle() {
        return windowHandle;
    }

    public void destroy() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
