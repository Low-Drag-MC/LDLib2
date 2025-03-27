package com.lowdragmc.lowdraglib.utils;

import com.lowdragmc.lowdraglib.LDLib;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;

/**
 * @author KilaBash
 * @date 2022/12/11
 * @implNote ColorUtils
 */
@UtilityClass
public final class ColorUtils {

    public static int randomColor(int minR, int maxR, int minG, int maxG, int minB, int maxB) {
        return 0xff000000 |
                ((minR + LDLib.RANDOM.nextInt(maxR + 1 - minR)) << 16) |
                ((minG + LDLib.RANDOM.nextInt(maxG + 1 - minG)) << 8) |
                ((minB + LDLib.RANDOM.nextInt(maxB + 1 - minB))) ;
    }

    public static int randomColor(int minA, int maxA, int minR, int maxR, int minG, int maxG, int minB, int maxB) {
        return  ((minR + LDLib.RANDOM.nextInt(maxA + 1 - minA)) << 24) |
                ((minR + LDLib.RANDOM.nextInt(maxR + 1 - minR)) << 16) |
                ((minG + LDLib.RANDOM.nextInt(maxG + 1 - minG)) << 8) |
                ((minB + LDLib.RANDOM.nextInt(maxB + 1 - minB))) ;
    }

    public static int randomColor(int colorA, int colorB) {
        return randomColor(Math.min(alphaI(colorA), alphaI(colorB)), Math.max(alphaI(colorA), alphaI(colorB)),
                Math.min(redI(colorA), redI(colorB)), Math.max(redI(colorA), redI(colorB)),
                Math.min(greenI(colorA), greenI(colorB)), Math.max(greenI(colorA), greenI(colorB)),
                Math.min(blueI(colorA), blueI(colorB)), Math.max(blueI(colorA), blueI(colorB)));
    }

    public static int randomColor() {
        return randomColor(0, 255, 0, 255,0, 255);
    }

    public static int averageColor(int... colors) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (int color : colors) {
            r += (color >> 16) & 0xFF;
            g += (color >> 8) & 0xFF;
            b += color & 0xFF;
        }
        return (r / colors.length) << 16 | (g / colors.length) << 8 | (b / colors.length);
    }

    public static double softLightBlend(double bg, double fg, double alphaBg, double alphaFg) {
        double newColor;
        if (fg <= 0.5) {
            newColor = 2 * bg * fg + bg * bg * (1 - 2 * fg);
        } else {
            newColor = Math.sqrt(bg) * (2 * fg - 1) + 2 * bg * (1 - fg);
        }

        newColor = alphaFg * newColor + alphaBg * (1 - alphaFg) * newColor;

        return newColor;
    }

    public static float alpha(int color) {
        return ((color >> 24) & 0xff) / 255f;
    }

    public static float red(int color) {
        return ((color >> 16) & 0xff) / 255f;
    }

    public static float green(int color) {
        return ((color >> 8) & 0xff) / 255f;
    }

    public static float blue(int color) {
        return ((color) & 0xff) / 255f;
    }

    public static int alphaI(int color) {
        return ((color >> 24) & 0xff);
    }

    public static int redI(int color) {
        return ((color >> 16) & 0xff);
    }

    public static int greenI(int color) {
        return ((color >> 8) & 0xff);
    }

    public static int blueI(int color) {
        return ((color) & 0xff);
    }

    public static int color(int alpha, int red, int green, int blue) {
        if (alpha > 255) alpha = 255;
        if (red > 255) red = 255;
        if (green > 255) green = 255;
        if (blue > 255) blue = 255;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static int color(float alpha, float red, float green, float blue) {
        return color((int)(alpha * 255), (int)(red * 255), (int)(green * 255), (int)(blue * 255));
    }

    public static int HSBtoRGB(float hue, float saturation, float brightness, float alpha) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0 -> {
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                }
                case 1 -> {
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                }
                case 2 -> {
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                }
                case 3 -> {
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                }
                case 4 -> {
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                }
                case 5 -> {
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                }
            }
        }
        return ((int) (alpha * 255)) << 24 | (r << 16) | (g << 8) | (b);
    }

    /**
     * all components should in [0-1]
     */
    public static float[] RGBtoHSB(int color) {
        int r = ((color >> 16) & 0xff);
        int g = ((color >> 8) & 0xff);
        int b = ((color) & 0xff);

        float hue, saturation, brightness;

        int cmax = Math.max(r, g);
        if (b > cmax) cmax = b;
        int cmin = Math.min(r, g);
        if (b < cmin) cmin = b;

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0)
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        return new float[]{hue, saturation, brightness};
    }

    public static int blendColor(int color0, int color1, float lerp) {
        return ColorUtils.color(
                Mth.lerp(lerp, alpha(color0), alpha(color1)),
                Mth.lerp(lerp, red(color0), red(color1)),
                Mth.lerp(lerp, green(color0), green(color1)),
                Mth.lerp(lerp, blue(color0), blue(color1))
        );
    }
}
