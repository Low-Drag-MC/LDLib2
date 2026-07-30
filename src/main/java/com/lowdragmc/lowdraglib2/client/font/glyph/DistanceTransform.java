package com.lowdragmc.lowdraglib2.client.font.glyph;

import org.jetbrains.annotations.Nullable;

/**
 * Converts a coverage mask into a signed distance field using the Felzenszwalb-Huttenlocher exact euclidean
 * distance transform (two separable O(n) passes).
 * <p>
 * Every glyph source goes through here. {@link #fromCoverage} recovers the sub-pixel outline position from
 * anti aliased coverage, which is what outline fonts provide; {@link #fromMask} is the fallback for pixel art
 * fonts, whose hard edges carry no sub-pixel information to recover.
 * <p>
 * One encoding is shared so that one shader can sample any of them: value 128 sits exactly on the outline and
 * one pixel of distance is worth {@code 128 / spread} steps.
 * <p>
 * This class is deliberately free of any Minecraft or OpenGL dependency so it can be unit tested.
 */
public final class DistanceTransform {
    /**
     * Stand-in for infinity. Kept finite so the parabola intersection arithmetic below never produces NaN.
     */
    private static final float INF = 1e20f;
    private static final float SQRT2 = 1.41421356f;

    public static final int ON_EDGE = 128;

    private DistanceTransform() {
    }

    /**
     * Builds a signed distance field from a coverage mask, expanding it by {@code padding} pixels on every side
     * so the field has room to fall off.
     *
     * @param coverage {@code srcWidth * srcHeight} bytes, a pixel counts as inside when its value is >= 128
     * @param srcWidth  mask width
     * @param srcHeight mask height
     * @param padding   pixels added on each side
     * @param spread    distance in pixels that maps to the full 0..128 range
     * @return {@code (srcWidth + 2 * padding) * (srcHeight + 2 * padding)} bytes
     */
    public static byte[] fromMask(byte[] coverage, int srcWidth, int srcHeight, int padding, float spread) {
        var width = srcWidth + padding * 2;
        var height = srcHeight + padding * 2;
        var toInside = new float[width * height];
        var toOutside = new float[width * height];

        for (int y = 0; y < height; y++) {
            var sy = y - padding;
            for (int x = 0; x < width; x++) {
                var sx = x - padding;
                var inside = sx >= 0 && sy >= 0 && sx < srcWidth && sy < srcHeight
                        && (coverage[sy * srcWidth + sx] & 0xFF) >= 128;
                var i = y * width + x;
                toInside[i] = inside ? 0f : INF;
                toOutside[i] = inside ? INF : 0f;
            }
        }

        transform(toInside, width, height);
        transform(toOutside, width, height);

        var result = new byte[width * height];
        var scale = ON_EDGE / spread;
        for (int i = 0; i < result.length; i++) {
            // positive inside the shape, negative outside
            var distance = (float) (Math.sqrt(toOutside[i]) - Math.sqrt(toInside[i]));
            var value = Math.round(ON_EDGE + distance * scale);
            result[i] = (byte) Math.max(0, Math.min(255, value));
        }
        return result;
    }

    /**
     * Nearest neighbour upscale of a coverage mask.
     * <p>
     * Bitmap based fonts are tiny (8 or 16 pixels tall), so their distance field would only have a handful of
     * samples across a stem. Upscaling before the transform does not invent detail, but it does let the field
     * describe the blocky outline accurately enough that the shader can anti alias it cleanly.
     */
    public static byte[] upscale(byte[] coverage, int width, int height, int factor) {
        if (factor <= 1) return coverage;
        var result = new byte[width * factor * height * factor];
        var scaledWidth = width * factor;
        for (int y = 0; y < height * factor; y++) {
            var sourceRow = (y / factor) * width;
            var targetRow = y * scaledWidth;
            for (int x = 0; x < scaledWidth; x++) {
                result[targetRow + x] = coverage[sourceRow + x / factor];
            }
        }
        return result;
    }

    /**
     * Builds a signed distance field from an <em>anti aliased</em> coverage bitmap, using the grey values to
     * recover where the outline actually runs inside each edge pixel.
     * <p>
     * This is what {@link #fromMask} cannot do. Thresholding coverage to black and white first snaps the zero
     * contour to pixel centres, so the field describes a staircase at the rasterization resolution rather than
     * the real outline. At small font sizes that staircase is far below a screen pixel and invisible, but it
     * scales with the text: at a 64 pixel font size a half texel step is over a screen pixel wide and reads as
     * obvious aliasing. Recovering the sub-pixel edge position removes it at every size.
     * <p>
     * The sub-pixel offset per edge pixel follows Gustavson and Strand's anti aliased distance transform: the
     * coverage gradient gives the edge direction, and the coverage value then gives how far the edge sits from
     * the pixel centre. Those offsets are carried out to every other pixel by tracking which edge pixel the
     * plain distance transform found nearest.
     *
     * @param coverage 8 bit coverage, 0 fully outside, 255 fully inside
     */
    public static byte[] fromCoverage(byte[] coverage, int srcWidth, int srcHeight, int padding, float spread) {
        var width = srcWidth + padding * 2;
        var height = srcHeight + padding * 2;
        var alpha = new float[width * height];
        for (int y = 0; y < srcHeight; y++) {
            for (int x = 0; x < srcWidth; x++) {
                alpha[(y + padding) * width + x + padding] = (coverage[y * srcWidth + x] & 0xFF) / 255f;
            }
        }

        // Signed distance from the pixel centre to the outline, positive inside, valid on seed pixels only.
        //
        // A pixel seeds the transform when it is partially covered, and also when a neighbour is on the other
        // side of the outline. That second case matters more than it looks: when the outline happens to run
        // along a pixel boundary there are no partially covered pixels there at all, and seeding only on
        // coverage would send those pixels off to some unrelated edge far away. For them half a pixel is both
        // the best available estimate and very nearly exact.
        var edgeDistance = new float[width * height];
        var seeds = new float[width * height];
        var partialCoverage = false;
        for (int i = 0; i < alpha.length; i++) {
            var a = alpha[i];
            if (a > 0f && a < 1f) {
                edgeDistance[i] = -edgeOffset(alpha, width, height, i, a);
                seeds[i] = 0f;
                partialCoverage = true;
            } else if (touchesOtherSide(alpha, width, height, i)) {
                edgeDistance[i] = a >= 0.5f ? 0.5f : -0.5f;
                seeds[i] = 0f;
            } else {
                seeds[i] = INF;
            }
        }
        if (!partialCoverage) {
            // a hard edged bitmap carries no sub-pixel information, nothing to recover
            return fromMask(coverage, srcWidth, srcHeight, padding, spread);
        }

        var nearest = new int[width * height];
        transform(seeds, nearest, width, height);

        var result = new byte[width * height];
        var scale = ON_EDGE / spread;
        for (int i = 0; i < result.length; i++) {
            var inside = alpha[i] >= 0.5f;
            // distance to the nearest edge pixel, then shifted by where the outline sits inside that pixel
            var distance = (inside ? 1f : -1f) * (float) Math.sqrt(seeds[i]) + edgeDistance[nearest[i]];
            var value = Math.round(ON_EDGE + distance * scale);
            result[i] = (byte) Math.max(0, Math.min(255, value));
        }
        return result;
    }

    /**
     * @return true when one of the four neighbours sits on the other side of the outline
     */
    private static boolean touchesOtherSide(float[] alpha, int width, int height, int index) {
        var inside = alpha[index] >= 0.5f;
        var x = index % width;
        var y = index / width;
        if (x > 0 && (alpha[index - 1] >= 0.5f) != inside) return true;
        if (x + 1 < width && (alpha[index + 1] >= 0.5f) != inside) return true;
        if (y > 0 && (alpha[index - width] >= 0.5f) != inside) return true;
        return y + 1 < height && (alpha[index + width] >= 0.5f) != inside;
    }

    /**
     * Distance from a pixel's centre to the outline crossing it, positive when the centre is outside.
     * <p>
     * Derived from the area a straight edge with the given direction has to cut off the unit pixel to leave
     * exactly {@code a} coverage. Everything is symmetric in sign and transposition, so the gradient is folded
     * into the first octant and only three cases remain.
     */
    private static float edgeOffset(float[] alpha, int width, int height, int index, float a) {
        var x = index % width;
        var y = index / width;
        if (x < 1 || y < 1 || x + 1 >= width || y + 1 >= height) {
            return 0.5f - a;
        }
        // rotated Sobel, which estimates the coverage gradient and therefore the edge normal
        var topLeft = alpha[index - width - 1];
        var top = alpha[index - width];
        var topRight = alpha[index - width + 1];
        var left = alpha[index - 1];
        var right = alpha[index + 1];
        var bottomLeft = alpha[index + width - 1];
        var bottom = alpha[index + width];
        var bottomRight = alpha[index + width + 1];
        var gx = -topLeft - SQRT2 * left - bottomLeft + topRight + SQRT2 * right + bottomRight;
        var gy = -topLeft - SQRT2 * top - topRight + bottomLeft + SQRT2 * bottom + bottomRight;

        if (gx == 0f || gy == 0f) {
            return 0.5f - a;
        }
        var length = (float) Math.sqrt(gx * gx + gy * gy);
        gx = Math.abs(gx / length);
        gy = Math.abs(gy / length);
        if (gx < gy) {
            var swap = gx;
            gx = gy;
            gy = swap;
        }
        var corner = 0.5f * gy / gx;
        if (a < corner) {
            return 0.5f * (gx + gy) - (float) Math.sqrt(2f * gx * gy * a);
        }
        if (a < 1f - corner) {
            return (0.5f - a) * gx;
        }
        return -0.5f * (gx + gy) + (float) Math.sqrt(2f * gx * gy * (1f - a));
    }

    /**
     * In place squared euclidean distance transform of a 2D sampled function.
     */
    private static void transform(float[] field, int width, int height) {
        transform(field, null, width, height);
    }

    /**
     * @param nearest optional output, receives the index of the nearest seed pixel for every pixel
     */
    private static void transform(float[] field, int @Nullable [] nearest, int width, int height) {
        var longest = Math.max(width, height);
        var source = new float[longest];
        var result = new float[longest];
        var argmin = new int[longest];
        var hullVertices = new int[longest];
        var hullBoundaries = new float[longest + 1];
        // the transform is separable, so the 2D nearest seed is recovered by composing the two 1D argmins
        var columnSource = nearest == null ? null : new int[width * height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                source[y] = field[y * width + x];
            }
            transform1d(source, height, result, argmin, hullVertices, hullBoundaries);
            for (int y = 0; y < height; y++) {
                field[y * width + x] = result[y];
                if (columnSource != null) {
                    columnSource[y * width + x] = argmin[y];
                }
            }
        }

        for (int y = 0; y < height; y++) {
            System.arraycopy(field, y * width, source, 0, width);
            transform1d(source, width, result, argmin, hullVertices, hullBoundaries);
            System.arraycopy(result, 0, field, y * width, width);
            if (nearest != null) {
                for (int x = 0; x < width; x++) {
                    var sourceX = argmin[x];
                    nearest[y * width + x] = columnSource[y * width + sourceX] * width + sourceX;
                }
            }
        }
    }

    /**
     * One dimensional squared distance transform: {@code result[q] = min over p of ((q - p)^2 + source[p])}.
     * Walks the lower envelope of the parabolas rooted at each sample.
     *
     * @param argmin receives the sample each result was minimised by
     */
    private static void transform1d(float[] source, int n, float[] result, int[] argmin,
                                    int[] hullVertices, float[] hullBoundaries) {
        if (n <= 0) return;
        var k = 0;
        hullVertices[0] = 0;
        hullBoundaries[0] = -INF;
        hullBoundaries[1] = INF;

        for (int q = 1; q < n; q++) {
            var s = intersection(source, q, hullVertices[k]);
            while (s <= hullBoundaries[k]) {
                k--;
                s = intersection(source, q, hullVertices[k]);
            }
            k++;
            hullVertices[k] = q;
            hullBoundaries[k] = s;
            hullBoundaries[k + 1] = INF;
        }

        k = 0;
        for (int q = 0; q < n; q++) {
            while (hullBoundaries[k + 1] < q) {
                k++;
            }
            var delta = q - hullVertices[k];
            result[q] = (float) delta * delta + source[hullVertices[k]];
            argmin[q] = hullVertices[k];
        }
    }

    /**
     * x coordinate where the parabolas rooted at {@code p} and {@code q} cross.
     */
    private static float intersection(float[] source, int q, int p) {
        return ((source[q] + (float) q * q) - (source[p] + (float) p * p)) / (2f * q - 2f * p);
    }
}
