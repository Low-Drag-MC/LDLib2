package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.editor.annotation.NumberColor;
import com.lowdragmc.lowdraglib.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.math.Rect;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector4f;

import java.awt.*;

@LDLRegisterClient(name = "color_rect_texture", registry = "ldlib:gui_texture")
@Accessors(chain = true)
public class ColorRectTexture extends TransformTexture{

    @Configurable
    @NumberColor
    @Setter
    public int color;

    @Configurable
    @Setter
    @NumberRange(range = {0, Float.MAX_VALUE}, wheel = 1)
    public float radiusLT;

    @Configurable
    @Setter
    @NumberRange(range = {0, Float.MAX_VALUE}, wheel = 1)
    public float radiusLB;

    @Configurable
    @Setter
    @NumberRange(range = {0, Float.MAX_VALUE}, wheel = 1)
    public float radiusRT;

    @Configurable
    @Setter
    @NumberRange(range = {0, Float.MAX_VALUE}, wheel = 1)
    public float radiusRB;

    public ColorRectTexture() {
        this(0x4f0ffddf);
    }

    public ColorRectTexture(int color) {
        this.color = color;
    }

    public ColorRectTexture(Color color) {
        this.color = color.getRGB();
    }

    public ColorRectTexture setRadius(float radius) {
        this.radiusLB = radius;
        this.radiusRT = radius;
        this.radiusRB = radius;
        this.radiusLT = radius;
        return this;
    }

    public ColorRectTexture setLeftRadius(float radius) {
        this.radiusLB = radius;
        this.radiusLT = radius;
        return this;
    }

    public ColorRectTexture setRightRadius(float radius) {
        this.radiusRT = radius;
        this.radiusRB = radius;
        return this;
    }

    public ColorRectTexture setTopRadius(float radius) {
        this.radiusRT = radius;
        this.radiusLT = radius;
        return this;
    }

    public ColorRectTexture setBottomRadius(float radius) {
        this.radiusLB = radius;
        this.radiusRB = radius;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        if (width == 0 || height == 0) return;
        if (radiusLT > 0 || radiusLB > 0 || radiusRT > 0 || radiusRB > 0) {
            float radius = Math.min(width, height) / 2f;
            DrawerHelper.drawRoundBox(graphics, Rect.ofRelative((int) x, width, (int) y, height),
                    new Vector4f(Math.min(radius, radiusRT), Math.min(radiusRB, radius), Math.min(radius, radiusLT), Math.min(radius, radiusLB)), color);
        } else {
            DrawerHelper.drawSolidRect(graphics, (int) x, (int) y, width, height, color);
        }
    }
}
