package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;

@KJSBindings
@LDLRegisterClient(name = "color_border_texture", registry = "ldlib2:gui_texture")
public class ColorBorderTexture extends TransformTexture{

    @Configurable
    @ConfigColor
    public int color;

    @Configurable
    @ConfigNumber(range = {-100, 100})
    public int border;

    public ColorBorderTexture() {
        this(-2, 0x4f0ffddf);
    }

    public ColorBorderTexture(int border, int color) {
        this.color = color;
        this.border = border;
    }

    public ColorBorderTexture(int border, java.awt.Color color) {
        this.color = color.getRGB();
        this.border = border;
    }

    public ColorBorderTexture setBorder(int border) {
        this.border = border;
        return this;
    }

    public ColorBorderTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public ColorBorderTexture copy() {
        var copied = new ColorBorderTexture(border, color);
        copied.copyTransform(this);
        return copied;
    }

    @Override
    public IGuiTexture interpolate(IGuiTexture other, float lerp) {
        if (other.getRawTexture() instanceof ColorBorderTexture colorRect) {
            return new ColorBorderTexture()
                    .setBorder((int) ((1 - lerp) * border + lerp * colorRect.border))
                    .setColor(ColorUtils.blendOklabColor(color, colorRect.color, lerp));
        }
        return super.interpolate(other, lerp);
    }

    @LDLRegisterClient(name = "color_border_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredColorBorderTextureRenderer implements RegisteredGuiTextureRenderer<ColorBorderTexture, RegisteredColorBorderTextureRenderer> {
        @Override
        public Class<ColorBorderTexture> type() {
            return ColorBorderTexture.class;
        }

        @Override
        public void draw(ColorBorderTexture texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(ColorBorderTexture texture, GUIContext context, float x, float y, float width, float height) {
            if (width <= 0 || height <= 0) {
                return;
            }
            if (texture.border >= 0) {
                DrawerHelperClient.drawSolidRect(context, x - texture.border, y + height, width + 2f * texture.border, texture.border, texture.color);
                DrawerHelperClient.drawSolidRect(context, x - texture.border, y, texture.border, height, texture.color);
                DrawerHelperClient.drawSolidRect(context, x + width, y, texture.border, height, texture.color);
                DrawerHelperClient.drawSolidRect(context, x - texture.border, y - texture.border, width + 2f * texture.border, texture.border, texture.color);
            } else {
                float absBorder = Math.abs(texture.border);
                DrawerHelperClient.drawSolidRect(context, x, y, width - absBorder, absBorder, texture.color);
                DrawerHelperClient.drawSolidRect(context, x, y + absBorder, absBorder, height - absBorder, texture.color);
                DrawerHelperClient.drawSolidRect(context, x + absBorder, y + height - absBorder, width - absBorder, absBorder, texture.color);
                DrawerHelperClient.drawSolidRect(context, x + width - absBorder, y, absBorder, height - absBorder, texture.color);
            }
        }
    }
}
