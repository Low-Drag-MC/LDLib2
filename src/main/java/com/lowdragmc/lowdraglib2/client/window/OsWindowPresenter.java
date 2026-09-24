package com.lowdragmc.lowdraglib2.client.window;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import org.jetbrains.annotations.Nullable;

/**
 * Puts a target rendered in the game's frame onto a second window's screen.
 *
 * <p>One path for every backend: {@code GpuDevice#createSurface} gives the window a surface of its own
 * and the sequence is the one {@code Minecraft#renderFrame} runs for the game's own window —
 * configure, acquire, blit, submit, present. Under Vulkan the surface is a swapchain; under OpenGL it
 * is the window's default framebuffer, reached by making the game's one context current on this
 * window, which renderpearl does itself. Everything upstream of here is shared too: the UI is recorded
 * and flushed into an ordinary {@code TextureTarget}, and only this last hop is per-window.
 *
 * <p>The submit between blit and present is not optional. {@code blitFromTexture} only
 * <em>records</em> the copy, and under Vulkan {@code present} waits on a semaphore that submission is
 * what signals; presenting without it waits on a semaphore nothing will ever signal. The game submits
 * once more at the end of its frame, which then simply starts from an empty batch.
 *
 * <p>Everything that can go wrong with a surface — the window resized under us, the compositor
 * changing format, a display being unplugged — arrives as a {@link SurfaceException} and is handled by
 * dropping the frame and rebuilding on the next one. That is what the game does too, and for a
 * secondary window a dropped frame costs nothing.
 */
public final class OsWindowPresenter {

    private final OsWindow window;
    @Nullable
    private GpuSurface surface;

    /**
     * Set whenever the surface has to be reconfigured before the next acquire: the first frame, a
     * resize, or an acquire that failed. Kept separate from {@link GpuSurface#isSuboptimal()} because
     * an out-of-date swapchain refuses to be acquired at all, so the flag has to survive the frame that
     * discovered it.
     */
    private boolean needsConfigure = true;

    public OsWindowPresenter(OsWindow window) {
        this.window = window;
    }

    /**
     * Blits {@code source}'s colour attachment into the window and presents it.
     *
     * <p>Silently does nothing when the window cannot be presented to right now — iconified, or a
     * surface that needs rebuilding. A dropped frame is the correct outcome; the next one will find the
     * window in a better state.
     */
    public void present(RenderTarget source) {
        if (window.isDestroyed()) return;
        var view = source.getColorTextureView();
        if (view == null) return;
        var width = window.getFramebufferWidth();
        var height = window.getFramebufferHeight();
        if (width <= 0 || height <= 0 || window.isIconified()) return;

        var current = surface;
        if (current == null) {
            current = RenderSystem.getDevice().createSurface(window.handle(), window::isIconified);
            surface = current;
            needsConfigure = true;
        }

        var configuration = current.currentConfiguration().orElse(null);
        if (needsConfigure || configuration == null || current.isSuboptimal()
                || configuration.width() != width || configuration.height() != height) {
            // Vsync off, deliberately. With it on, presenting this window blocks the render thread —
            // the game's — on this window's vertical blank as well as its own, which halves the
            // framerate of everything.
            var mode = GpuSurface.PresentMode.getSupportedVsyncMode(current.supportedPresentModes(), false);
            try {
                current.configure(new GpuSurface.Configuration(width, height, mode));
                needsConfigure = false;
            } catch (SurfaceException exception) {
                // Routinely transient: a window mid-resize reports an extent the surface does not
                // accept yet. Debug rather than warn, or a drag across a monitor edge floods the log.
                LDLib2.LOGGER.debug("[os-window] could not configure the surface at {}x{}", width, height, exception);
                needsConfigure = true;
                return;
            }
        }

        try {
            current.acquireNextTexture();
        } catch (SurfaceException exception) {
            LDLib2.LOGGER.debug("[os-window] could not acquire a surface texture", exception);
            needsConfigure = true;
            return;
        }

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        current.blitFromTexture(encoder, view);
        encoder.submit();
        current.present();
    }

    /**
     * Releases the surface. Must run before the window is destroyed — the surface belongs to it and
     * cannot outlive it.
     */
    public void destroy() {
        var current = surface;
        surface = null;
        if (current == null) return;
        try {
            current.close();
        } catch (Throwable throwable) {
            // A surface still holding an acquired image refuses to close. Nothing can release it from
            // out here, so report and drop it rather than let a window that is already going away
            // take the teardown — and the frame it is running on — down with it.
            LDLib2.LOGGER.warn("[os-window] could not close the surface", throwable);
        }
    }
}
