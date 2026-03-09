package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatHSBRectRenderState;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import net.minecraft.client.gui.render.TextureSetup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ColorSelectorClientTextures {
    private static final ColorSelectorTextures.Provider PROVIDER = new ColorSelectorTextures.Provider() {
        @Override
        public IGuiTexture colorPreview(ColorSelector selector) {
            return GuiTexture.of((context, x, y, width, height) -> drawColorPreview(selector, context, x, y, width, height));
        }

        @Override
        public IGuiTexture hsbContext(ColorSelector selector) {
            return GuiTexture.of((context, x, y, width, height) -> drawHsbContext(selector, context, x, y, width, height));
        }

        @Override
        public IGuiTexture colorSlider(ColorSelector selector) {
            return GuiTexture.of((context, x, y, width, height) -> drawColorSlider(selector, context, x, y, width, height));
        }

        @Override
        public IGuiTexture alphaSlider(ColorSelector selector) {
            return GuiTexture.of((context, x, y, width, height) -> drawAlphaSlider(selector, context, x, y, width, height));
        }
    };

    private ColorSelectorClientTextures() {}

    public static void init() {
        ColorSelectorTextures.register(PROVIDER);
    }

    private static void drawColorPreview(ColorSelector selector, GUIContext context, float x, float y, float width, float height) {
        DrawerHelperClient.drawSolidRect(context, x, y, width, height, selector.getColor());
    }

    private static void drawHsbContext(ColorSelector selector, GUIContext context, float x, float y, float width, float height) {
        float[] tl;
        float[] bl;
        float[] br;
        float[] tr;
        switch (selector.mode) {
            case H -> {
                tl = new float[]{selector.h, 0f, 1f, 1f};
                bl = new float[]{selector.h, 0f, 0f, 1f};
                br = new float[]{selector.h, 1f, 0f, 1f};
                tr = new float[]{selector.h, 1f, 1f, 1f};
            }
            case S -> {
                tl = new float[]{0f, selector.s, 1f, 1f};
                bl = new float[]{0f, selector.s, 0f, 1f};
                br = new float[]{360f, selector.s, 0f, 1f};
                tr = new float[]{360f, selector.s, 1f, 1f};
            }
            case B -> {
                tl = new float[]{0f, 1f, selector.b, 1f};
                bl = new float[]{0f, 0f, selector.b, 1f};
                br = new float[]{360f, 0f, selector.b, 1f};
                tr = new float[]{360f, 1f, selector.b, 1f};
            }
            default -> {
                return;
            }
        }
        context.submitGuiElement(new FloatHSBRectRenderState(
                LDLibRenderPipelines.HSB, TextureSetup.noTexture(), context.pose.copyPose(),
                x, y, x + width, y + height, tl, bl, br, tr, context.peekScissor()));

        float mainX = 0;
        float mainY = 0;
        switch (selector.mode) {
            case H -> {
                mainX = selector.s;
                mainY = 1 - selector.b;
            }
            case S -> {
                mainX = selector.h / 360f;
                mainY = 1 - selector.b;
            }
            case B -> {
                mainX = selector.h / 360f;
                mainY = 1 - selector.s;
            }
        }
        DrawerHelperClient.drawSolidRect(context, (x + mainX * width) - 1, (y + mainY * height) - 1, 2, 2,
                selector.b > 0.5f ? 0xff000000 : 0xffffffff);
    }

    private static void drawColorSlider(ColorSelector selector, GUIContext context, float x, float y, float width, float height) {
        float[] top;
        float[] bottom;
        switch (selector.mode) {
            case H -> {
                top = new float[]{360f, 1f, 1f, 1f};
                bottom = new float[]{0f, 1f, 1f, 1f};
            }
            case S -> {
                top = new float[]{selector.h, 1f, selector.b, 1f};
                bottom = new float[]{selector.h, 0f, selector.b, 1f};
            }
            case B -> {
                top = new float[]{selector.h, selector.s, 1f, 1f};
                bottom = new float[]{selector.h, selector.s, 0f, 1f};
            }
            default -> {
                return;
            }
        }
        context.submitGuiElement(new FloatHSBRectRenderState(
                LDLibRenderPipelines.HSB, TextureSetup.noTexture(), context.pose.copyPose(),
                x, y, x + width, y + height, top, bottom, bottom, top, context.peekScissor()));

        float normalizedPos = switch (selector.mode) {
            case H -> 1 - selector.h / 360f;
            case S -> 1 - selector.s;
            case B -> 1 - selector.b;
        };
        DrawerHelperClient.drawSolidRect(context, (x - 2), (y + normalizedPos * height), width + 4, 1, 0xffff0000);
    }

    private static void drawAlphaSlider(ColorSelector selector, GUIContext context, float x, float y, float width, float height) {
        DrawerHelperClient.drawGradientRect(context, x, y, width, height, selector.argb & 0x00ffffff, selector.argb | 0xff000000, true);
        DrawerHelperClient.drawSolidRect(context, (x + selector.alpha * width), (y - 2), 1, (height + 4), 0xffff0000);
    }
}
