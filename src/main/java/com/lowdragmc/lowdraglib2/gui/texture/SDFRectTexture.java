package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector4f;


@KJSBindings
@LDLRegisterClient(name = "sdf_rect_texture", registry = "ldlib2:gui_texture")
@Accessors(chain = true)
public class SDFRectTexture extends TransformTexture {
    @Getter
    @Configurable
    @ConfigNumber(range = {0f, Float.MAX_VALUE}, wheel = 1)
    private Vector4f radius = new Vector4f(0, 0, 0, 0);
    @Getter @Setter
    @Configurable
    @ConfigNumber(range = {0f, Float.MAX_VALUE}, wheel = 1)
    private float stroke = 0;
    @Getter
    @Configurable
    @ConfigColor
    private int color = 0xFFFFFFFF;
    @Getter
    @Configurable
    @ConfigColor
    private int borderColor = 0xff000000;

    public static SDFRectTexture of(int color) {
        return new SDFRectTexture().setColor(color);
    }

    @Override
    @ConfigSetter(field = "color")
    public SDFRectTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @ConfigSetter(field = "borderColor")
    public SDFRectTexture setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        return this;
    }

    public SDFRectTexture setRadius(float radius) {
        return setRadius(new Vector4f(radius));
    }

    public SDFRectTexture setRadius(Vector4f radius) {
        this.radius = radius;
        return this;
    }

    @Override
    public SDFRectTexture copy() {
        var copied = new SDFRectTexture();
        copied.setRadius(new Vector4f(radius));
        copied.setStroke(stroke);
        copied.setColor(color);
        copied.setBorderColor(borderColor);
        copied.copyTransform(this);
        return copied;
    }

    @Override
    public IGuiTexture interpolate(IGuiTexture other, float lerp) {
        if (other.getRawTexture() instanceof SDFRectTexture sdfRect) {
            var blended = new SDFRectTexture();
            blended.setRadius(new Vector4f(radius).lerp(sdfRect.getRadius(), lerp));
            blended.setStroke((1 - lerp) * stroke + sdfRect.stroke * lerp);
            blended.setColor(ColorUtils.blendOklabColor(color, sdfRect.color, lerp));
            blended.setBorderColor(ColorUtils.blendOklabColor(borderColor, sdfRect.borderColor, lerp));
            blended.copyTransform(Transform2D.interpolate(getTransform2D(), sdfRect.getTransform2D(), lerp));
            return blended;
        }
        return super.interpolate(other, lerp);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GUIContext context, float x, float y, float width, float height) {
        if (ColorUtils.alpha(color) > 0) {
            context.fillRoundedRect(x, y, width, height, radius, color);
        }
        if (stroke > 0 && ColorUtils.alpha(borderColor) > 0) {
            context.borderRoundedRect(x, y, width, height, radius, stroke, borderColor);
        }
    }
}
