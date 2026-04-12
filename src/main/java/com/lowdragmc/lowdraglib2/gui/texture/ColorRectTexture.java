package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

@LDLRegisterClient(name = "color_rect_texture", registry = "ldlib2:gui_texture")
@Accessors(chain = true)
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

    @Environment(EnvType.CLIENT)
    @Override
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        if (width <= 0 || height <= 0) return;

        DrawerHelper.drawSolidRect(graphics, x, y, width, height, color);
    }
}
