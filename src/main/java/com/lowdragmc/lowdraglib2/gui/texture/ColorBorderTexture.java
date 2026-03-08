package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
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
}
