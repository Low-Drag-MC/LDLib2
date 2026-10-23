package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.uitest.capture.FrameCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;

/** Reads back what a {@link Scene} in the game window drew, for scenarios that check its rendering. */
final class ScenePixels {

    /** Half the side of the square read around the scene's centre, in framebuffer pixels. */
    private static final int HALF = 15;
    /** Summed per-channel distance under which two pixels count as the same. */
    private static final int TOLERANCE = 12;

    private ScenePixels() {
    }

    /** The square around the scene's centre in the last frame, row by row. */
    static int[] centre(Scene scene) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int cx = (int) ((scene.getPositionX() + scene.getSizeWidth() / 2) * guiScale);
        int cy = (int) ((scene.getPositionY() + scene.getSizeHeight() / 2) * guiScale);
        var image = FrameCapture.grab();
        try {
            int side = HALF * 2 + 1;
            var pixels = new int[side * side];
            for (int dy = -HALF; dy <= HALF; dy++) {
                for (int dx = -HALF; dx <= HALF; dx++) {
                    pixels[(dy + HALF) * side + dx + HALF] = image.getPixel(cx + dx, cy + dy);
                }
            }
            return pixels;
        } finally {
            FrameCapture.closeQuietly(image);
        }
    }

    /** The share of pixels that differ between two reads of {@link #centre}, from 0 to 1. */
    static float changed(int[] a, int[] b) {
        int changed = 0;
        for (int i = 0; i < a.length; i++) {
            if (difference(a[i], b[i]) > TOLERANCE) changed++;
        }
        return changed / (float) a.length;
    }

    /** The share of pixels in a read of {@link #centre} that differ from {@code colour}, from 0 to 1. */
    static float notColour(int[] pixels, int colour) {
        int changed = 0;
        for (int pixel : pixels) {
            if (difference(pixel, colour) > TOLERANCE) changed++;
        }
        return changed / (float) pixels.length;
    }

    private static int difference(int a, int b) {
        return Math.abs(ARGB.red(a) - ARGB.red(b))
                + Math.abs(ARGB.green(a) - ARGB.green(b))
                + Math.abs(ARGB.blue(a) - ARGB.blue(b));
    }
}
