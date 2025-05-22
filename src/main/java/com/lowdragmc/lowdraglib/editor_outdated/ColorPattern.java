package com.lowdragmc.lowdraglib.editor_outdated;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.utils.ColorUtils;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote ColorPattern
 */
public enum ColorPattern {
    WHITE(0xffffffff),
    T_WHITE(0x88ffffff),
    BLACK(0xff222222),
    T_BLACK(0x44222222),
    SEAL_BLACK(0xFF313638),
    T_SEAL_BLACK(0x88313638),
    GRAY(0xff666666),
    T_GRAY(0x66666666),
    DARK_GRAY(0xff444444),
    T_DARK_GRAY(0x44444444),
    LIGHT_GRAY(0xffaaaaaa),
    T_LIGHT_GRAY(0x88aaaaaa),

    GREEN(0xff33ff00),
    T_GREEN(0x8833ff00),
    RED(0xff9d0122),
    T_RED(0x889d0122),
    BRIGHT_RED(0xffFF0000),
    T_BRIGHT_RED(0x88FF0000),
    YELLOW(0xffffff33),
    T_YELLOW(0x88ffff33),
    CYAN(0xff337777),
    T_CYAN(0x88337777),
    PURPLE(0xff9933ff),
    T_PURPLE(0x889933ff),
    PINK(0xffff33ff),
    T_PINK(0x88ff33ff),
    BLUE(0xff4852ff),
    T_BLUE(0x884852ff),
    ORANGE(0xffff8800),
    T_ORANGE(0x88ff8800),
    BROWN(0xffaa7744),
    T_BROWN(0x88aa7744),
    LIME(0xff77aa44),
    T_LIME(0x8877aa44),
    MAGENTA(0xffaa44aa),
    T_MAGENTA(0x88aa44aa),
    LIGHT_BLUE(0xff44aaff),
    T_LIGHT_BLUE(0x8844aaff),
    ;
    public final int color;

    ColorPattern(int color) {
        this.color = color;
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
