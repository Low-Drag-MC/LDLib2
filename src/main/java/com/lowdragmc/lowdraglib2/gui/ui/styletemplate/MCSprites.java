package com.lowdragmc.lowdraglib2.gui.ui.styletemplate;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.UIResourceTexture;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Modifier;

public class MCSprites {
    public static ResourceLocation MC = LDLib2.id("textures/gui/mc_styles.png");

    public static IGuiTexture RECT = SpriteTexture.of(MC).setSprite(0, 0, 16, 16).setBorder(5);
    public static IGuiTexture RECT_INVERSE = SpriteTexture.of(MC).setSprite(16, 0, 16, 16).setBorder(3);
    public static IGuiTexture RECT_BORDER = SpriteTexture.of(MC).setSprite(32, 0, 16, 16).setBorder(3);
    public static IGuiTexture RECT_THIN = SpriteTexture.of(MC).setSprite(48, 0, 16, 16).setBorder(3);
    public static IGuiTexture BORDER = SpriteTexture.of(MC).setSprite(64, 0, 16, 16).setBorder(5);
    public static IGuiTexture BORDER_1 = SpriteTexture.of(MC).setSprite(80, 0, 16, 16).setBorder(6);
    public static IGuiTexture BORDER_2 = SpriteTexture.of(MC).setSprite(96, 0, 16, 16).setBorder(7);
    public static IGuiTexture BORDER_4 = SpriteTexture.of(MC).setSprite(112, 0, 16, 16).setBorder(6);

    public static IGuiTexture RECT_1 = SpriteTexture.of(MC).setSprite(0, 16, 16, 16).setBorder(2);
    public static IGuiTexture RECT_2 = SpriteTexture.of(MC).setSprite(16, 16, 16, 16).setBorder(2);
    public static IGuiTexture RECT_3 = SpriteTexture.of(MC).setSprite(32, 16, 16, 16).setBorder(2);
    public static IGuiTexture RECT_4 = SpriteTexture.of(MC).setSprite(48, 16, 16, 16).setBorder(2);
    public static IGuiTexture RECT_5 = SpriteTexture.of(MC).setSprite(64, 16, 16, 16).setBorder(2);
    public static IGuiTexture RECT_6 = SpriteTexture.of(MC).setSprite(80, 16, 16, 16).setBorder(1);

    public static IGuiTexture TAB_OFF = SpriteTexture.of(MC).setSprite(0, 32, 16, 16).setBorder(4);
    public static IGuiTexture TAB_ON = SpriteTexture.of(MC).setSprite(16, 32, 16, 16).setBorder(4);
    public static IGuiTexture RECT_BLACK = SpriteTexture.of(MC).setSprite(32, 32, 16, 16).setBorder(3);

    public static IGuiTexture SCROLLER_V = SpriteTexture.of(MC).setSprite(0, 48, 16, 16).setBorder(2).setWrapMode(SpriteTexture.WrapMode.REPEAT);
    public static IGuiTexture SCROLLER_V_DARK = SpriteTexture.of(MC).setSprite(16, 48, 16, 16).setBorder(2).setWrapMode(SpriteTexture.WrapMode.REPEAT);
    public static IGuiTexture SCROLLER_H = SpriteTexture.of(MC).setSprite(32, 48, 16, 16).setBorder(2).setWrapMode(SpriteTexture.WrapMode.REPEAT);
    public static IGuiTexture SCROLLER_H_DARK = SpriteTexture.of(MC).setSprite(48, 48, 16, 16).setBorder(2).setWrapMode(SpriteTexture.WrapMode.REPEAT);
    public static IGuiTexture SWITCH = SpriteTexture.of(MC).setSprite(64, 48, 16, 16).setBorder(2);


    public static void init(ResourceInstance<IGuiTexture> instance) {
        var provider = new BuiltinResourceProvider<>("ui-mc", instance);
        for (var field : MCSprites.class.getDeclaredFields()) {
            if (IGuiTexture.class.isAssignableFrom(field.getType())
                    && Modifier.isStatic(field.getModifiers()) ) {
                try {
                    var texture = (IGuiTexture) field.get(null);
                    provider.addResource(field.getName(), texture);
                    var res = new BuiltinPath("ui-mc:" + field.getName());
                    texture = new UIResourceTexture(res);
                    field.set(null, texture);
                } catch (Exception ignored) {}
            }
        }
        instance.addBuiltinProvider(provider);
    }
}
