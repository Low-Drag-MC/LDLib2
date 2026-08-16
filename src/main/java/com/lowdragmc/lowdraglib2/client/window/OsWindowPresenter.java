package com.lowdragmc.lowdraglib2.client.window;

import com.mojang.blaze3d.pipeline.RenderTarget;

/**
 * Puts a target rendered in the game's frame onto a second window's screen.
 *
 * <p>Two implementations, because this is the one part of hosting a UI in another window that the
 * graphics backend actually changes — see {@link OsWindowBackend}. Everything upstream of here is
 * shared: the UI is recorded and flushed into an ordinary {@code TextureTarget} through
 * {@code RenderSystem}, and only the last hop differs.
 */
public interface OsWindowPresenter {

    /**
     * Blits {@code source}'s colour attachment into the window and swaps.
     *
     * <p>Takes the target rather than a texture id so neither implementation has to be described in
     * the other's terms: one wants a raw GL name, the other a {@code GpuTextureView}, and both are
     * on here.
     *
     * <p>Silently does nothing when the window cannot be presented to right now — iconified, or a
     * swapchain that needs rebuilding. A dropped frame is the correct outcome; the next one will
     * find the window in a better state.
     */
    void present(RenderTarget source);

    /**
     * Releases whatever the presenter allocated against the window. Must run before the window is
     * destroyed — both implementations own objects that belong to it and cannot outlive it.
     */
    void destroy();

    /**
     * The presenter for {@code window}, matching the backend the game is running on.
     */
    static OsWindowPresenter create(OsWindow window) {
        return OsWindowBackend.current().isOpenGl()
                ? new GlBlitPresenter(window)
                : new GpuSurfacePresenter(window);
    }
}
