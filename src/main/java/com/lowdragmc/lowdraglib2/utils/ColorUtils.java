package com.lowdragmc.lowdraglib2.utils;

import com.lowdragmc.lowdraglib2.LDLib2;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import org.joml.Vector4f;

/**
 * @author KilaBash
 * @date 2022/12/11
 * @implNote ColorUtils
 */
@UtilityClass
public final class ColorUtils {

    public static int randomColor(int minR, int maxR, int minG, int maxG, int minB, int maxB) {
        return 0xff000000 |
                ((minR + LDLib2.RANDOM.nextInt(maxR + 1 - minR)) << 16) |
                ((minG + LDLib2.RANDOM.nextInt(maxG + 1 - minG)) << 8) |
                ((minB + LDLib2.RANDOM.nextInt(maxB + 1 - minB))) ;
    }

    public static int randomColor(int minA, int maxA, int minR, int maxR, int minG, int maxG, int minB, int maxB) {
        return  ((minR + LDLib2.RANDOM.nextInt(maxA + 1 - minA)) << 24) |
                ((minR + LDLib2.RANDOM.nextInt(maxR + 1 - minR)) << 16) |
                ((minG + LDLib2.RANDOM.nextInt(maxG + 1 - minG)) << 8) |
                ((minB + LDLib2.RANDOM.nextInt(maxB + 1 - minB))) ;
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

    public static int color(double alpha, double red, double green, double blue) {
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

    public static int blendRGBColor(int from, int to, float lerp) {
        return ColorUtils.color(
                Mth.lerp(lerp, alpha(from), alpha(to)),
                Mth.lerp(lerp, red(from), red(to)),
                Mth.lerp(lerp, green(from), green(to)),
                Mth.lerp(lerp, blue(from), blue(to))
        );
    }

    public static int blendOklabColor(int from, int to, float lerp) {
        // Get RGB components
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int fa = (from >> 24) & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int ta = (to >> 24) & 0xFF;

        // Convert to linear sRGB
        double[] fromLinear = {fr / 255.0, fg / 255.0, fb / 255.0};
        double[] toLinear = {tr / 255.0, tg / 255.0, tb / 255.0};

        // Convert to OKLAB
        double[] fromOklab = rgbToOklab(fromLinear);
        double[] toOklab = rgbToOklab(toLinear);

        // Interpolate in OKLAB
        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            result[i] = fromOklab[i] + (toOklab[i] - fromOklab[i]) * lerp;
        }

        // Convert back to RGB
        double[] rgb = oklabToRGB(result);

        // Convert to 8-bit color components
        int r = (int) Math.round(rgb[0] * 255);
        int g = (int) Math.round(rgb[1] * 255);
        int b = (int) Math.round(rgb[2] * 255);
        int a = (int) (fa + (ta - fa) * lerp);

        // Clamp values
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));
        a = Math.min(255, Math.max(0, a));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public double[] rgbToOklab(double[] rgb) {
        // Convert to linear sRGB to OKLAB
        double l = 0.4122214708 * rgb[0] + 0.5363325363 * rgb[1] + 0.0514459929 * rgb[2];
        double m = 0.2119034982 * rgb[0] + 0.6806995451 * rgb[1] + 0.1073969566 * rgb[2];
        double s = 0.0883024619 * rgb[0] + 0.2817188376 * rgb[1] + 0.6299787005 * rgb[2];

        l = Math.cbrt(l);
        m = Math.cbrt(m);
        s = Math.cbrt(s);

        return new double[]{
                0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
                1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
                0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        };
    }

    public double[] oklabToRGB(double[] oklab) {
        double l = oklab[0] + 0.3963377774 * oklab[1] + 0.2158037573 * oklab[2];
        double m = oklab[0] - 0.1055613458 * oklab[1] - 0.0638541728 * oklab[2];
        double s = oklab[0] - 0.0894841775 * oklab[1] - 1.2914855480 * oklab[2];

        l = l * l * l;
        m = m * m * m;
        s = s * s * s;

        return new double[]{
                +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
                -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
                -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
        };
    }

    public double[] rgbToHSL(double[] rgb) {
        double r = rgb[0];
        double g = rgb[1];
        double b = rgb[2];

        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double h, s, l = (max + min) / 2.0;

        if (max == min) {
            h = s = 0.0; // achromatic
        } else {
            double d = max - min;
            s = l > 0.5 ? d / (2.0 - max - min) : d / (max + min);

            if (max == r) {
                h = (g - b) / d + (g < b ? 6.0 : 0.0);
            } else if (max == g) {
                h = (b - r) / d + 2.0;
            } else {
                h = (r - g) / d + 4.0;
            }

            h /= 6.0;
        }

        return new double[]{h, s, l};
    }

    public double[] hslToRGB(double[] hsl) {
        double h = hsl[0];
        double s = hsl[1];
        double l = hsl[2];

        double r, g, b;

        if (s == 0) {
            r = g = b = l; // achromatic
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;

            r = hueToRGB(p, q, h + 1.0 / 3.0);
            g = hueToRGB(p, q, h);
            b = hueToRGB(p, q, h - 1.0 / 3.0);
        }

        return new double[]{r, g, b};
    }

    private double hueToRGB(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }

    public static int fromVector4f(Vector4f color) {
        return ColorUtils.color(color.w, color.x, color.y, color.z);
    }

    public static Vector4f toVector4f(int color) {
        return new Vector4f(red(color), green(color), blue(color), alpha(color));
    }

    public static int addColor(int color0, int color1) {
        return ColorUtils.color(
                alpha(color0) + alpha(color1),
                red(color0) + red(color1),
                green(color0) + green(color1),
                blue(color0) + blue(color1)
        );
    }

    public static int subColor(int color0, int color1) {
        return ColorUtils.color(
                alpha(color0) - alpha(color1),
                red(color0) - red(color1),
                green(color0) - green(color1),
                blue(color0) - blue(color1)
        );
    }

    public static int mulColor(int color0, int color1) {
        return ColorUtils.color(
                alpha(color0) * alpha(color1),
                red(color0) * red(color1),
                green(color0) * green(color1),
                blue(color0) * blue(color1)
        )                                         ;
    }

    public static Integer parseColor(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim().toLowerCase();

        try {
            if (value.startsWith("#")) {
                String hex = value.substring(1);
                switch (hex.length()) {
                    case 3: // #RGB
                        int r = Integer.parseInt(hex.substring(0, 1), 16) * 17; // F -> FF
                        int g = Integer.parseInt(hex.substring(1, 2), 16) * 17;
                        int b = Integer.parseInt(hex.substring(2, 3), 16) * 17;
                        return 0xFF000000 | (r << 16) | (g << 8) | b;
                    case 6: // #RRGGBB
                        return 0xFF000000 | Integer.parseInt(hex, 16);
                    case 8: // #AARRGGBB
                        return (int) (Long.parseLong(hex, 16) & 0xFFFFFFFFL);
                    default:
                        return null;
                }
            } else if (value.startsWith("rgb(") && value.endsWith(")")) {
                String[] parts = value.substring(4, value.length() - 1).split(",");
                if (parts.length != 3) return null;

                int r = parseColorComponent(parts[0].trim());
                int g = parseColorComponent(parts[1].trim());
                int b = parseColorComponent(parts[2].trim());

                if (r < 0 || g < 0 || b < 0) return null;

                return 0xFF000000 | (r << 16) | (g << 8) | b;
            } else if (value.startsWith("rgba(") && value.endsWith(")")) {
                String[] parts = value.substring(5, value.length() - 1).split(",");
                if (parts.length != 4) return null;

                int r = parseColorComponent(parts[0].trim());
                int g = parseColorComponent(parts[1].trim());
                int b = parseColorComponent(parts[2].trim());
                float alpha = Float.parseFloat(parts[3].trim());

                if (r < 0 || g < 0 || b < 0 || alpha < 0.0f || alpha > 1.0f) return null;

                int a = Math.round(alpha * 255);
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    public static int parseColorComponent(String component) {
        try {
            int value = Integer.parseInt(component);
            return (value >= 0 && value <= 255) ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}
