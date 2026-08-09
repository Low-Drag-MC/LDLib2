package com.lowdragmc.lowdraglib2.uitest.capture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the current frame back from the GPU and writes PNGs.
 *
 * <p>One download per capture: the full frame, every element crop and any future comparison all
 * derive from the same {@link NativeImage}. A readback is a GPU stall, so doing it once matters.
 *
 * <p>Writes are synchronous rather than queued on {@code Util.ioPool()}. The run exits immediately
 * after the report is written, and asynchronous writes would race that exit and silently drop the
 * most interesting screenshot — the one from the step that failed.
 */
@OnlyIn(Dist.CLIENT)
public final class FrameCapture {

    private FrameCapture() {
    }

    /**
     * Grabs the main render target.
     *
     * <p>Must be called after the frame has rendered — the runner only ever steps from a post-render
     * hook — otherwise the image is whatever was on screen before this frame's UI was drawn.
     *
     * @return the image, which the caller owns and must close
     */
    public static NativeImage grab() {
        var target = Minecraft.getInstance().getMainRenderTarget();
        var image = new NativeImage(target.width, target.height, false);
        RenderSystem.bindTexture(target.getColorTextureId());
        image.downloadTexture(0, true);
        // The framebuffer is bottom-up; PNG is top-down.
        image.flipY();
        return image;
    }

    /**
     * Writes the image as a PNG, creating parent directories.
     *
     * @return {@code true} if the image was a single flat colour, which almost always means the
     *         wrong framebuffer was bound rather than that the UI really is one colour. Surfacing it
     *         beats letting someone stare at a black PNG wondering what broke.
     */
    public static boolean write(NativeImage image, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        image.writeToFile(destination);
        return isUniform(image);
    }

    /**
     * Crops out an element's rectangle, converting from GUI-scaled coordinates to framebuffer pixels.
     *
     * @param padding extra pixels of context around the element, in GUI units
     * @return the cropped image, which the caller owns, or {@code null} if the rectangle does not
     *         intersect the frame at all
     */
    @Nullable
    public static NativeImage crop(NativeImage frame, ElementBounds bounds, float padding) {
        var window = Minecraft.getInstance().getWindow();
        double scaleX = frame.getWidth() / (double) Math.max(1, window.getGuiScaledWidth());
        double scaleY = frame.getHeight() / (double) Math.max(1, window.getGuiScaledHeight());

        int x0 = (int) Math.floor((bounds.x() - padding) * scaleX);
        int y0 = (int) Math.floor((bounds.y() - padding) * scaleY);
        int x1 = (int) Math.ceil((bounds.right() + padding) * scaleX);
        int y1 = (int) Math.ceil((bounds.bottom() + padding) * scaleY);

        x0 = Math.max(0, Math.min(x0, frame.getWidth()));
        y0 = Math.max(0, Math.min(y0, frame.getHeight()));
        x1 = Math.max(0, Math.min(x1, frame.getWidth()));
        y1 = Math.max(0, Math.min(y1, frame.getHeight()));

        int width = x1 - x0;
        int height = y1 - y0;
        if (width <= 0 || height <= 0) return null;

        var cropped = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cropped.setPixelRGBA(x, y, frame.getPixelRGBA(x0 + x, y0 + y));
            }
        }
        return cropped;
    }

    /**
     * Samples the image on a coarse grid rather than reading every pixel — a 4K frame is 8 million
     * pixels and this runs on the render thread.
     */
    private static boolean isUniform(NativeImage image) {
        if (image.getWidth() < 2 || image.getHeight() < 2) return true;
        int first = image.getPixelRGBA(0, 0);
        int stepX = Math.max(1, image.getWidth() / 32);
        int stepY = Math.max(1, image.getHeight() / 32);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                if (image.getPixelRGBA(x, y) != first) return false;
            }
        }
        return true;
    }

    /** Closes an image, logging rather than throwing — a capture must never break a run. */
    public static void closeQuietly(@Nullable NativeImage image) {
        if (image == null) return;
        try {
            image.close();
        } catch (Exception e) {
            LDLib2.LOGGER.warn("[uitest] failed to release a captured image", e);
        }
    }
}
