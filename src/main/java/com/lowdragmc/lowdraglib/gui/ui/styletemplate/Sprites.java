package com.lowdragmc.lowdraglib.gui.ui.styletemplate;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.texture.SpriteTexture;
import net.minecraft.resources.ResourceLocation;

public class Sprites {
    public static ResourceLocation GDP = LDLib.id("textures/gui/gdp_styles.png");

    public static SpriteTexture RECT_RD = SpriteTexture.of(GDP).setSprite(1, 29, 13, 13).setBorder(4, 4, 4, 4);
    public static SpriteTexture RECT_RD_LIGHT = SpriteTexture.of(GDP).setSprite(1, 15, 13, 13).setBorder(4, 4, 4, 4);
    public static SpriteTexture RECT_RD_DARK = SpriteTexture.of(GDP).setSprite(1, 43, 13, 13).setBorder(4, 4, 4, 4);
    public static SpriteTexture RECT_RD_SOLID = SpriteTexture.of(GDP).setSprite(1, 1, 13, 13).setBorder(2, 2, 2, 2);

    public static SpriteTexture RECT_RD_T = SpriteTexture.of(GDP).setSprite(15, 29, 13, 13).setBorder(4, 4, 4, 4);
    public static SpriteTexture RECT_RD_T_LIGHT = SpriteTexture.of(GDP).setSprite(15, 15, 13, 13).setBorder(4, 4, 4, 4);
    public static SpriteTexture RECT_RD_T_DARK = SpriteTexture.of(GDP).setSprite(15, 43, 13, 13).setBorder(4, 4, 4, 4);
    public static SpriteTexture RECT_RD_T_SOLID = SpriteTexture.of(GDP).setSprite(15, 1, 13, 13).setBorder(4, 4, 4, 4);

    public static SpriteTexture RECT = SpriteTexture.of(GDP).setSprite(29, 29, 13, 13).setBorder(4, 4, 4, 4);
    public static SpriteTexture RECT_LIGHT = SpriteTexture.of(GDP).setSprite(29, 15, 13, 13).setBorder(4, 4, 4, 4);
    public static SpriteTexture RECT_DARK = SpriteTexture.of(GDP).setSprite(29, 43, 13, 13).setBorder(4, 4, 4, 4);
    public static SpriteTexture RECT_SOLID = SpriteTexture.of(GDP).setSprite(29, 1, 13, 13).setBorder(1, 1, 1, 1);

    public static SpriteTexture BORDER = SpriteTexture.of(GDP).setSprite(86, 131, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER_DARK = SpriteTexture.of(GDP).setSprite(86, 148, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER_TRANSLATE = SpriteTexture.of(GDP).setSprite(86, 165, 16, 16).setBorder(3, 3, 3, 3);

    public static SpriteTexture BORDER_THICK = SpriteTexture.of(GDP).setSprite(171, 154, 16, 16).setBorder(6, 6, 6, 6);
    public static SpriteTexture BORDER_THICK_DARK = SpriteTexture.of(GDP).setSprite(171, 171, 16, 16).setBorder(6, 6, 6, 6);
    public static SpriteTexture BORDER_THICK_TRANSLATE = SpriteTexture.of(GDP).setSprite(171, 165, 16, 16).setBorder(4, 4, 4, 4);

    public static SpriteTexture BORDER_RT0 = SpriteTexture.of(GDP).setSprite(103, 131, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER_RT0_DARK = SpriteTexture.of(GDP).setSprite(103, 148, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER_RT0_TRANSLATE = SpriteTexture.of(GDP).setSprite(103, 165, 16, 16).setBorder(3, 3, 3, 3);

    public static SpriteTexture BORDER_THICK_RT0 = SpriteTexture.of(GDP).setSprite(188, 154, 16, 16).setBorder(6, 6, 6, 6);
    public static SpriteTexture BORDER_THICK_RT0_DARK = SpriteTexture.of(GDP).setSprite(188, 171, 16, 16).setBorder(6, 6, 6, 6);
    public static SpriteTexture BORDER_THICK_RT0_TRANSLATE = SpriteTexture.of(GDP).setSprite(188, 165, 16, 16).setBorder(4, 4, 4, 4);

    public static SpriteTexture BORDER_RT1 = SpriteTexture.of(GDP).setSprite(120, 131, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER_RT1_DARK = SpriteTexture.of(GDP).setSprite(120, 148, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER_RT1_TRANSLATE = SpriteTexture.of(GDP).setSprite(120, 165, 16, 16).setBorder(3, 3, 3, 3);

    public static SpriteTexture BORDER_THICK_RT1 = SpriteTexture.of(GDP).setSprite(205, 154, 16, 16).setBorder(6, 6, 6, 6);
    public static SpriteTexture BORDER_THICK_RT1_DARK = SpriteTexture.of(GDP).setSprite(205, 171, 16, 16).setBorder(6, 6, 6, 6);
    public static SpriteTexture BORDER_THICK_RT1_TRANSLATE = SpriteTexture.of(GDP).setSprite(205, 165, 16, 16).setBorder(4, 4, 4, 4);

    public static SpriteTexture BORDER_RT2 = SpriteTexture.of(GDP).setSprite(137, 131, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER_RT2_DARK = SpriteTexture.of(GDP).setSprite(137, 148, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER_RT2_TRANSLATE = SpriteTexture.of(GDP).setSprite(137, 165, 16, 16).setBorder(3, 3, 3, 3);

    public static SpriteTexture BORDER_THICK_RT2 = SpriteTexture.of(GDP).setSprite(222, 154, 16, 16).setBorder(6, 6, 6, 6);
    public static SpriteTexture BORDER_THICK_RT2_DARK = SpriteTexture.of(GDP).setSprite(222, 171, 16, 16).setBorder(6, 6, 6, 6);
    public static SpriteTexture BORDER_THICK_RT2_TRANSLATE = SpriteTexture.of(GDP).setSprite(222, 165, 16, 16).setBorder(4, 4, 4, 4);

    public static SpriteTexture BORDER_RT3 = SpriteTexture.of(GDP).setSprite(154, 131, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER_RT3_DARK = SpriteTexture.of(GDP).setSprite(154, 148, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER_RT3_TRANSLATE = SpriteTexture.of(GDP).setSprite(154, 165, 16, 16).setBorder(3, 3, 3, 3);

    public static SpriteTexture BORDER1 = SpriteTexture.of(GDP).setSprite(86, 205, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_DARK = SpriteTexture.of(GDP).setSprite(86, 222, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_TRANSLATE = SpriteTexture.of(GDP).setSprite(86, 239, 16, 16).setBorder(3, 5, 3, 3);

    public static SpriteTexture BORDER1_THICK = SpriteTexture.of(GDP).setSprite(171, 205, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_THICK_DARK = SpriteTexture.of(GDP).setSprite(171, 222, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_THICK_TRANSLATE = SpriteTexture.of(GDP).setSprite(171, 239, 16, 16).setBorder(3, 5, 3, 3);

    public static SpriteTexture BORDER1_RT0 = SpriteTexture.of(GDP).setSprite(103, 205, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_RT0_DARK = SpriteTexture.of(GDP).setSprite(103, 222, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_RT0_TRANSLATE = SpriteTexture.of(GDP).setSprite(103, 239, 16, 16).setBorder(3, 3, 3, 3);

    public static SpriteTexture BORDER1_THICK_RT0 = SpriteTexture.of(GDP).setSprite(188, 205, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_THICK_RT0_DARK = SpriteTexture.of(GDP).setSprite(188, 222, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_THICK_RT0_TRANSLATE = SpriteTexture.of(GDP).setSprite(188, 239, 16, 16).setBorder(3, 5, 3, 3);

    public static SpriteTexture BORDER1_RT1 = SpriteTexture.of(GDP).setSprite(120, 205, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_RT1_DARK = SpriteTexture.of(GDP).setSprite(120, 222, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_RT1_TRANSLATE = SpriteTexture.of(GDP).setSprite(120, 165, 16, 16).setBorder(3, 3, 3, 3);

    public static SpriteTexture BORDER1_THICK_RT1 = SpriteTexture.of(GDP).setSprite(205, 205, 16, 16).setBorder(6, 6, 6, 6);
    public static SpriteTexture BORDER1_THICK_RT1_DARK = SpriteTexture.of(GDP).setSprite(205, 222, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_THICK_RT1_TRANSLATE = SpriteTexture.of(GDP).setSprite(205, 239, 16, 16).setBorder(3, 5, 3, 3);

    public static SpriteTexture BORDER1_RT2 = SpriteTexture.of(GDP).setSprite(137, 205, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_RT2_DARK = SpriteTexture.of(GDP).setSprite(137, 222, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_RT2_TRANSLATE = SpriteTexture.of(GDP).setSprite(137, 239, 16, 16).setBorder(3, 3, 3, 3);

    public static SpriteTexture BORDER1_THICK_RT2 = SpriteTexture.of(GDP).setSprite(222, 205, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_THICK_RT2_DARK = SpriteTexture.of(GDP).setSprite(222, 222, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_THICK_RT2_TRANSLATE = SpriteTexture.of(GDP).setSprite(222, 239, 16, 16).setBorder(3, 5, 3, 3);

    public static SpriteTexture BORDER1_RT3 = SpriteTexture.of(GDP).setSprite(154, 205, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_RT3_DARK = SpriteTexture.of(GDP).setSprite(154, 222, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_RT3_TRANSLATE = SpriteTexture.of(GDP).setSprite(154, 239, 16, 16).setBorder(3, 3, 3, 3);

    public static SpriteTexture BORDER1_THICK_RT3 = SpriteTexture.of(GDP).setSprite(239, 205, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_THICK_RT3_DARK = SpriteTexture.of(GDP).setSprite(239, 222, 16, 16).setBorder(5, 5, 5, 5);
    public static SpriteTexture BORDER1_THICK_RT3_TRANSLATE = SpriteTexture.of(GDP).setSprite(239, 239, 16, 16).setBorder(3, 5, 3, 3);

    public static SpriteTexture SCROLL_CONTAINER_V = SpriteTexture.of(GDP).setSprite(48, 198, 5, 7).setBorder(2, 2, 2, 2);
    public static SpriteTexture SCROLL_BAR_V = SpriteTexture.of(GDP).setSprite(48, 174, 5, 7).setBorder(2, 2, 2, 2);
    public static SpriteTexture SCROLL_BAR_LIGHT_V = SpriteTexture.of(GDP).setSprite(48, 182, 5, 7).setBorder(2, 2, 2, 2);
    public static SpriteTexture SCROLL_BAR_WHITE_V = SpriteTexture.of(GDP).setSprite(48, 190, 5, 7).setBorder(2, 2, 2, 2);

    public static SpriteTexture SCROLL_CONTAINER_H = SpriteTexture.of(GDP).setSprite(55, 200, 7, 5).setBorder(2, 2, 2, 2);
    public static SpriteTexture SCROLL_BAR_H = SpriteTexture.of(GDP).setSprite(55, 182, 7, 5).setBorder(2, 2, 2, 2);
    public static SpriteTexture SCROLL_BAR_LIGHT_H = SpriteTexture.of(GDP).setSprite(55, 188, 7, 5).setBorder(2, 2, 2, 2);
    public static SpriteTexture SCROLL_BAR_WHITE_H = SpriteTexture.of(GDP).setSprite(55, 194, 7, 5).setBorder(2, 2, 2, 2);

    public static SpriteTexture PROGRESS_CONTAINER = SpriteTexture.of(GDP).setSprite(237, 130, 18, 11).setBorder(4, 4, 4, 4);
    public static SpriteTexture PROGRESS_BAR = SpriteTexture.of(GDP).setSprite(241, 164, 10, 3).setBorder(1, 1, 1, 1);

    public static SpriteTexture TAB = SpriteTexture.of(GDP).setSprite(242, 85, 13, 13).setBorder(3, 3, 3, 3);
    public static SpriteTexture TAB_DARK = SpriteTexture.of(GDP).setSprite(242, 71, 13, 13).setBorder(3, 3, 3, 3);
    public static SpriteTexture TAB_WHITE = SpriteTexture.of(GDP).setSprite(242, 113, 13, 13).setBorder(3, 3, 3, 3);

}
