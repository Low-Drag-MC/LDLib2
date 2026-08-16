package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import org.jetbrains.annotations.Nullable;

/**
 * A UI destination that is not the game window: a render target of its own, presented somewhere else.
 *
 * <p>The gui scale is deliberately <em>not</em> independent — it reads the game window's. Font
 * rasterisation picks its glyph bucket from {@code Minecraft.getWindow().getGuiScale()} deep inside
 * the text engine, with no way to pass a different one down, so a surface with its own scale would
 * render text at the wrong weight and spacing. Sharing the scale keeps every existing pixel
 * calculation valid; only the target size differs.
 */
public final class OffscreenSurface implements UISurface {

    @Nullable
    private TextureTarget target;
    private final long windowHandle;
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
            // With depth: the UI itself is flat, but a scene element drawn inside it is not, and it
            // renders into whatever target is bound.
            // 26.2: the colour format is declared up front. RGBA8_UNORM matches the game's own main
            // target, so the pipelines the gui renderer draws with are the ones they were built for
            // and the blit into the other window's default framebuffer needs no conversion.
            target = new TextureTarget("ldlib2 ui surface", width, height, true, GpuFormat.RGBA8_UNORM);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height);
        }
    }

    @Override
    public RenderTarget target() {
        if (target == null) {
            throw new IllegalStateException("Surface has been destroyed");
        }
        return target;
    }

    @Override
    public double guiScale() {
        return MainWindowSurface.INSTANCE.guiScale();
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
