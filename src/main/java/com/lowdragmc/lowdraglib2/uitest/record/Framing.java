package com.lowdragmc.lowdraglib2.uitest.record;

import org.jetbrains.annotations.Nullable;

/**
 * Where a take looks and how big its video is. Rectangles are {@code x, y, width, height} in GUI-scaled
 * screen coordinates.
 */
final class Framing {

    private Framing() {
    }

    /** The height of a key in the key display, for a region this tall. */
    static float keyHeight(float regionHeight) {
        return Math.max(14f, Math.min(24f, regionHeight * 0.05f));
    }

    /** Room the key display needs below a cropped region, so it does not cover the UI. */
    static float keySpace(float regionHeight) {
        return keyHeight(regionHeight) * 2f;
    }

    /** Room a burned-in caption needs below a cropped region. */
    static float captionSpace(float regionHeight) {
        return keyHeight(regionHeight) * 1.7f;
    }

    /**
     * The part of the screen a take records: the whole screen, or {@code target} (an element's bounds, or
     * the options' own rectangle) with padding and room below for the key display and caption; grown to
     * the video's aspect ratio when the options fix its size; always inside the screen.
     */
    static float[] region(TakeOptions options, @Nullable float[] target, float screenWidth, float screenHeight) {
        var rect = target != null ? target : options.regionRect;
        float[] region;
        if (rect == null) {
            region = new float[]{0, 0, screenWidth, screenHeight};
        } else {
            float height = rect[3] + options.padding * 2;
            float strip = 0;
            if (options.keys && options.keyStrip) strip += keySpace(height);
            if (options.burnCaptions != null) strip += captionSpace(height);
            region = new float[]{rect[0] - options.padding, rect[1] - options.padding, rect[2] + options.padding * 2, height + strip};
        }
        if (options.width > 0 && options.height > 0) {
            region = fitAspect(region, options.width / (float) options.height, screenWidth, screenHeight);
        }
        return clamp(region, screenWidth, screenHeight);
    }

    /** The video's size in pixels, even in both directions; {@code null} when it would be smaller than 2x2. */
    @Nullable
    static int[] outputSize(TakeOptions options, float[] region, double guiScale) {
        int width, height;
        if (options.width > 0 && options.height > 0) {
            width = options.width & ~1;
            height = options.height & ~1;
        } else {
            width = (int) (region[2] * guiScale * options.scale) & ~1;
            height = (int) (region[3] * guiScale * options.scale) & ~1;
        }
        return width < 2 || height < 2 ? null : new int[]{width, height};
    }

    /**
     * Where the camera goes to show {@code rect}: padded, taller at the bottom when the caption and keys
     * need room there, grown to the video's aspect ratio, inside the screen.
     */
    static float[] zoom(float[] rect, float padding, boolean reserveBottom, float aspect, float screenWidth, float screenHeight) {
        float height = rect[3] + padding * 2;
        if (reserveBottom) height *= 1.18f;
        var frame = fitAspect(new float[]{rect[0] - padding, rect[1] - padding, rect[2] + padding * 2, height},
                aspect, screenWidth, screenHeight);
        return clamp(frame, screenWidth, screenHeight);
    }

    /** Grows a rectangle around its centre to {@code aspect}, then shrinks it uniformly if it no longer fits the screen. */
    static float[] fitAspect(float[] rect, float aspect, float screenWidth, float screenHeight) {
        float x = rect[0], y = rect[1], w = Math.max(rect[2], 1e-3f), h = Math.max(rect[3], 1e-3f);
        if (w / h < aspect) {
            float grown = h * aspect;
            x -= (grown - w) / 2;
            w = grown;
        } else {
            float grown = w / aspect;
            y -= (grown - h) / 2;
            h = grown;
        }
        float shrink = Math.min(1, Math.min(screenWidth / w, screenHeight / h));
        if (shrink < 1) {
            x += w * (1 - shrink) / 2;
            y += h * (1 - shrink) / 2;
            w *= shrink;
            h *= shrink;
        }
        return new float[]{x, y, w, h};
    }

    /** No larger than the screen, moved (not cut) to lie inside it. */
    static float[] clamp(float[] rect, float screenWidth, float screenHeight) {
        float w = Math.min(rect[2], screenWidth);
        float h = Math.min(rect[3], screenHeight);
        float x = Math.max(0, Math.min(rect[0], screenWidth - w));
        float y = Math.max(0, Math.min(rect[1], screenHeight - h));
        return new float[]{x, y, w, h};
    }
}
