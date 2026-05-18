package com.lowdragmc.lowdraglib2.gui.ui.styletemplate;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.UIResourceTexture;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Modifier;

public class OreSprites {
    public static ResourceLocation ORE = LDLib2.id("textures/gui/ore_styles.png");

    public static IGuiTexture BTN_DEFAULT = SpriteTexture.of(ORE).setSprite(0, 0, 5, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture BTN_PRESSED = SpriteTexture.of(ORE).setSprite(5, 0, 5, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture BTN_DISABLED = SpriteTexture.of(ORE).setSprite(10, 0, 5, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture BTN_DEFAULT_GREEN = SpriteTexture.of(ORE).setSprite(26, 0, 5, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture BTN_PRESSED_GREEN = SpriteTexture.of(ORE).setSprite(31, 0, 5, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture BTN_DEFAULT_RED = SpriteTexture.of(ORE).setSprite(36, 0, 5, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture BTN_PRESSED_RED = SpriteTexture.of(ORE).setSprite(41, 0, 5, 7).setBorder(2, 2, 2, 4);

    public static IGuiTexture BTN_DEFAULT_SMALL = SpriteTexture.of(ORE).setSprite(46, 0, 5, 5).setBorder(2);
    public static IGuiTexture BTN_HOVER_SMALL = SpriteTexture.of(ORE).setSprite(51, 0, 5, 5).setBorder(2);
    public static IGuiTexture BTN_PRESSED_SMALL = SpriteTexture.of(ORE).setSprite(56, 0, 5, 5).setBorder(2);
    public static IGuiTexture BTN_DISABLED_SMALL = SpriteTexture.of(ORE).setSprite(61, 0, 3, 3).setBorder(1);
    public static IGuiTexture BTN_DEFAULT_SMALL_GREEN = SpriteTexture.of(ORE).setSprite(64, 0, 5, 5).setBorder(2);
    public static IGuiTexture BTN_HOVER_SMALL_GREEN = SpriteTexture.of(ORE).setSprite(69, 0, 5, 5).setBorder(2);
    public static IGuiTexture BTN_PRESSED_SMALL_GREEN = SpriteTexture.of(ORE).setSprite(74, 0, 5, 5).setBorder(2);
    public static IGuiTexture BTN_DEFAULT_SMALL_RED = SpriteTexture.of(ORE).setSprite(79, 0, 5, 5).setBorder(2);
    public static IGuiTexture BTN_HOVER_SMALL_RED = SpriteTexture.of(ORE).setSprite(84, 0, 5, 5).setBorder(2);
    public static IGuiTexture BTN_PRESSED_SMALL_RED = SpriteTexture.of(ORE).setSprite(89, 0, 5, 5).setBorder(2);

    public static IGuiTexture BTN_RECT_DEFAULT = SpriteTexture.of(ORE).setSprite(0, 7, 13, 14).setBorder(2, 2, 2, 3);
    public static IGuiTexture BTN_RECT_DISABLED = SpriteTexture.of(ORE).setSprite(0, 21, 13, 14).setBorder(2, 2, 2, 3);
    public static IGuiTexture BTN_RECT_HOVER = SpriteTexture.of(ORE).setSprite(0, 35, 13, 14).setBorder(2, 2, 2, 3);

    public static IGuiTexture SLOT_LIGHT = SpriteTexture.of(ORE).setSprite(15, 0, 3, 3).setBorder(1);
    public static IGuiTexture SLOT_GRAY = SpriteTexture.of(ORE).setSprite(18, 0, 3, 3).setBorder(1);
    public static IGuiTexture RECT = SpriteTexture.of(ORE).setSprite(21, 0, 3, 4).setBorder(1, 1, 1, 2);
    public static IGuiTexture RECT2 = SpriteTexture.of(ORE).setSprite(24, 0, 3, 5).setBorder(1, 3, 1, 1);
    public static IGuiTexture WHITE_BORDER = SpriteTexture.of(ORE).setSprite(15, 3, 3, 3).setBorder(1);

    public static IGuiTexture SWITCH_ON = SpriteTexture.of(ORE).setSprite(13, 7, 24, 14);
    public static IGuiTexture SWITCH_OFF = SpriteTexture.of(ORE).setSprite(13, 21, 24, 14);

    public static IGuiTexture TAB_OFF_DEFAULT = SpriteTexture.of(ORE).setSprite(50, 7, 11, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture TAB_OFF_HOVER = SpriteTexture.of(ORE).setSprite(50, 14, 11, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture TAB_OFF_PRESSED = SpriteTexture.of(ORE).setSprite(50, 21, 11, 7).setBorder(2, 4, 2, 2);
    public static IGuiTexture TAB_OFF_DISABLED = SpriteTexture.of(ORE).setSprite(50, 28, 11, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture TAB_ON_DEFAULT = SpriteTexture.of(ORE).setSprite(61, 7, 11, 7).setBorder(2, 4, 2, 2);
    public static IGuiTexture TAB_ON_HOVER = SpriteTexture.of(ORE).setSprite(61, 14, 11, 7).setBorder(2, 4, 2, 2);
    public static IGuiTexture TAB_ON_PRESSED = SpriteTexture.of(ORE).setSprite(61, 21, 11, 7).setBorder(2, 4, 2, 2);
    public static IGuiTexture TAB_ON_DISABLED = SpriteTexture.of(ORE).setSprite(61, 28, 11, 7).setBorder(2, 4, 2, 2);

    public static IGuiTexture TAB_OFF_DEFAULT_GREEN = SpriteTexture.of(ORE).setSprite(72, 7, 11, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture TAB_OFF_HOVER_GREEN = SpriteTexture.of(ORE).setSprite(72, 7, 11, 7).setBorder(2, 2, 2, 4);
    public static IGuiTexture TAB_ON_DEFAULT_GREEN = SpriteTexture.of(ORE).setSprite(83, 14, 11, 7).setBorder(2, 4, 2, 2);
    public static IGuiTexture TAB_ON_HOVER_GREEN = SpriteTexture.of(ORE).setSprite(83, 14, 11, 7).setBorder(2, 4, 2, 2);


    public static IGuiTexture BORDER = SpriteTexture.of(ORE).setSprite(0, 71, 62, 64).setBorder(5, 5, 5, 7);
    public static IGuiTexture BORDER_2 = SpriteTexture.of(ORE).setSprite(62, 71, 62, 64).setBorder(5, 5, 5, 7);
    public static IGuiTexture BORDER_3 = SpriteTexture.of(ORE).setSprite(0, 135 , 62, 64).setBorder(5, 5, 5, 7);
    public static IGuiTexture BORDER_4 = SpriteTexture.of(ORE).setSprite(62, 135, 62, 64).setBorder(5, 5, 5, 7);
    public static IGuiTexture BORDER_5 = SpriteTexture.of(ORE).setSprite(0, 199, 50, 251 - 199).setBorder(3, 3, 3, 5);
    public static IGuiTexture BORDER_6 = SpriteTexture.of(ORE).setSprite(128, 1, 128, 128).setBorder(6, 6, 6, 8);
    public static IGuiTexture BORDER_7 = SpriteTexture.of(ORE).setSprite(128, 128, 128, 128).setBorder(3, 3, 3, 5);

    public static IGuiTexture CHECK = SpriteTexture.of(ORE).setSprite(50, 35, 10, 10);
    public static IGuiTexture DOWN = SpriteTexture.of(ORE).setSprite(60, 35, 10, 10);

    public static void init(ResourceInstance<IGuiTexture> instance) {
        var provider = new BuiltinResourceProvider<>("ui-ore", instance);
        for (var field : OreSprites.class.getDeclaredFields()) {
            if (IGuiTexture.class.isAssignableFrom(field.getType())
                    && Modifier.isStatic(field.getModifiers()) ) {
                try {
                    var texture = (IGuiTexture) field.get(null);
                    provider.addResource(field.getName(), texture);
                    var res = new BuiltinPath("ui-ore:" + field.getName());
                    texture = new UIResourceTexture(res);
                    field.set(null, texture);
                } catch (Exception ignored) {}
            }
        }
        instance.addBuiltinProvider(provider);
    }
}
