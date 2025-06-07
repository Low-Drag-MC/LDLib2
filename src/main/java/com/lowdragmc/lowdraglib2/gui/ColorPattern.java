package com.lowdragmc.lowdraglib2.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote ColorPattern
 */
public enum ColorPattern {
    WHITE(0xffffffff, "white"),
    T_WHITE(0x88ffffff, "transparent white"),
    BLACK(0xff222222, "black"),
    T_BLACK(0x44222222, "transparent black"),
    SEAL_BLACK(0xFF313638, "seal black"),
    T_SEAL_BLACK(0x88313638, "transparent seal black"),
    GRAY(0xff666666, "gray"),
    T_GRAY(0x66666666, "transparent gray"),
    DARK_GRAY(0xff444444, "dark gray"),
    T_DARK_GRAY(0x44444444, "transparent dark gray"),
    LIGHT_GRAY(0xffaaaaaa, "light gray"),
    T_LIGHT_GRAY(0x88aaaaaa, "transparent light gray"),

    GREEN(0xff33ff00, "green"),
    T_GREEN(0x8833ff00, "transparent green"),
    RED(0xff9d0122, "red"),
    T_RED(0x889d0122, "transparent red"),
    BRIGHT_RED(0xffFF0000, "bright red"),
    T_BRIGHT_RED(0x88FF0000, "transparent bright red"),
    YELLOW(0xffffff33, "yellow"),
    T_YELLOW(0x88ffff33, "transparent yellow"),
    CYAN(0xff337777, "cyan"),
    T_CYAN(0x88337777, "transparent cyan"),
    PURPLE(0xff9933ff, "purple"),
    T_PURPLE(0x889933ff, "transparent purple"),
    PINK(0xffff33ff, "pink"),
    T_PINK(0x88ff33ff, "transparent pink"),
    BLUE(0xff4852ff, "blue"),
    T_BLUE(0x884852ff, "transparent blue"),
    ORANGE(0xffff8800, "orange"),
    T_ORANGE(0x88ff8800, "transparent orange"),
    BROWN(0xffaa7744, "brown"),
    T_BROWN(0x88aa7744, "transparent brown"),
    LIME(0xff77aa44, "lime"),
    T_LIME(0x8877aa44, "transparent lime"),
    MAGENTA(0xffaa44aa, "magenta"),
    T_MAGENTA(0x88aa44aa, "transparent magenta"),
    LIGHT_BLUE(0xff44aaff, "light blue"),
    T_LIGHT_BLUE(0x8844aaff, "transparent light blue"),
    SLATE_PLUM(0xff47434f, "slate plum"),;
    ;
    public final int color;
    public final String colorName;

    ColorPattern(int color, String colorName) {
        this.color = color;
        this.colorName = colorName;
    }

    public ColorRectTexture rectTexture() {
        return new ColorRectTexture(color);
    }

    public ColorBorderTexture borderTexture(int border) {
        return new ColorBorderTexture(border, color);
    }

    public static int generateRainbowColor(long tick) {
        float hue = (tick % 70) / 70f;
        int rgb = ColorUtils.HSBtoRGB(hue, 1.0f, 1.0f, 1.0f);
        return (0xff << 24) | (rgb & 0x00FFFFFF);
    }

    public static int generateRainbowColor() {
        float hue = (System.currentTimeMillis() % 3600) / 3600f;
        int rgb = ColorUtils.HSBtoRGB(hue, 1.0f, 1.0f, 1.0f);
        return (0xff << 24) | (rgb & 0x00FFFFFF);
    }

    public static ColorRectTexture rainbowRectTexture() {
        return new ColorRectTexture(generateRainbowColor());
    }

    public static ColorBorderTexture rainbowRectTexture(int border) {
        return new ColorBorderTexture(border, generateRainbowColor());
    }

}
