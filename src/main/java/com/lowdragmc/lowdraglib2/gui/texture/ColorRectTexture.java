package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import lombok.Setter;
import lombok.experimental.Accessors;

@LDLRegisterClient(name = "color_rect_texture", registry = "ldlib2:gui_texture")
@Accessors(chain = true)
@KJSBindings
public class ColorRectTexture extends TransformTexture {
    @Configurable
    @ConfigColor
    @Setter
    public int color;

    public ColorRectTexture() {
        this(0x4f0ffddf);
    }

    public ColorRectTexture(int color) {
        this.color = color;
    }

    public ColorRectTexture(java.awt.Color color) {
        this.color = color.getRGB();
    }

    @Override
    public ColorRectTexture copy() {
        var copied = new ColorRectTexture(color);
        copied.copyTransform(this);
        return copied;
    }

    @Override
    public IGuiTexture interpolate(IGuiTexture other, float lerp) {
        if (other.getRawTexture() instanceof ColorRectTexture colorRect) {
            return new ColorRectTexture().setColor(ColorUtils.blendOklabColor(color, colorRect.color, lerp));
        }
        return super.interpolate(other, lerp);
    }

    @LDLRegisterClient(name = "color_rect_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredColorRectTextureRenderer implements RegisteredGuiTextureRenderer<ColorRectTexture, RegisteredColorRectTextureRenderer> {
        @Override
        public Class<ColorRectTexture> type() {
            return ColorRectTexture.class;
        }

        @Override
        public void draw(ColorRectTexture texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(ColorRectTexture texture, GUIContext context, float x, float y, float width, float height) {
            if (width <= 0 || height <= 0) {
                return;
            }
            DrawerHelperClient.drawSolidRect(context, x, y, width, height, texture.color);
        }
    }
}
