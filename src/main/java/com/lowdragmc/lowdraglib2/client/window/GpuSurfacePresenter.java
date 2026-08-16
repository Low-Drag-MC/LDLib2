package com.lowdragmc.lowdraglib2.client.window;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SurfaceException;
import org.jetbrains.annotations.Nullable;

/**
 * {@link OsWindowBackend#SURFACE}: gives the window a swapchain of its own and blits into it.
 *
 * <p>This is the path a Vulkan game takes, and it is a good deal less exotic than its OpenGL
 * counterpart. There is no second context to make current and no raw GL: {@code GpuDevice#createSurface}
 * builds a {@code VkSurfaceKHR} straight from the GLFW window handle, and from there the sequence is
 * the same one {@code Minecraft#renderFrame} runs for the game's own window — configure, acquire,
 * blit, submit, present.
 *
 * <p>The submit between blit and present is not optional. {@code blitFromTexture} only <em>records</em>
 * the copy into a transient command buffer, and {@code present} waits on a semaphore that submission
 * is what signals; presenting without it waits on a semaphore nothing will ever signal. The game
 * submits once more at the end of its frame, which then simply starts from an empty batch.
 *
 * <p>Everything that can go wrong with a swapchain — the window resized under us, the compositor
 * changing format, a display being unplugged — arrives as a {@link SurfaceException} and is handled
 * by dropping the frame and rebuilding on the next one. That is what the game does too, and for a
 * secondary window a dropped frame costs nothing.
 */
public final class GpuSurfacePresenter implements OsWindowPresenter {

    private final OsWindow window;
    @Nullable
    private GpuSurface surface;

    /**
     * Set whenever the swapchain has to be rebuilt before the next acquire: the first frame, a resize,
     * or an acquire that failed. Kept separate from {@link GpuSurface#isSuboptimal()} because an
     * out-of-date swapchain refuses to be acquired at all, so the flag has to survive the frame that
     * discovered it.
     */
    private boolean needsConfigure = true;

    public GpuSurfacePresenter(OsWindow window) {
        this.window = window;
    }

    @Override
    public void present(RenderTarget source) {
        if (window.isDestroyed()) return;
        var view = source.getColorTextureView();
        if (view == null) return;
        var width = window.getFramebufferWidth();
        var height = window.getFramebufferHeight();
        if (width <= 0 || height <= 0) return; // iconified: there is no swapchain extent to target

        var current = surface;
        if (current == null) {
            current = RenderSystem.getDevice().createSurface(window.handle());
            surface = current;
            needsConfigure = true;
        }

        var configuration = current.currentConfiguration().orElse(null);
        if (needsConfigure || configuration == null || current.isSuboptimal()
                || configuration.width() != width || configuration.height() != height) {
            // Vsync off, deliberately. Swapchain present modes are per-surface, and leaving a second
            // window on FIFO makes the render thread — the game's — block on that window's vertical
            // blank as well as its own, which halves the framerate of everything.
            var mode = GpuSurface.PresentMode.getSupportedVsyncMode(current.supportedPresentModes(), false);
            try {
                current.configure(new GpuSurface.Configuration(width, height, mode));
                needsConfigure = false;
            } catch (SurfaceException exception) {
                // Routinely transient: a window mid-resize reports an extent the surface does not
                // accept yet. Debug rather than warn, or a drag across a monitor edge floods the log.
                LDLib2.LOGGER.debug("[os-window] could not configure the swapchain at {}x{}", width, height, exception);
                needsConfigure = true;
                return;
            }
        }

        try {
            current.acquireNextTexture();
        } catch (SurfaceException exception) {
            LDLib2.LOGGER.debug("[os-window] could not acquire a swapchain image", exception);
            needsConfigure = true;
            return;
        }

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        current.blitFromTexture(encoder, view);
        encoder.submit();
        current.present();
    }

    @Override
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
            LDLib2.LOGGER.warn("[os-window] could not close the swapchain", throwable);
        }
    }
}
