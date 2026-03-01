package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector4f;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.*;

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
    // runtime
    private Vector4f colorVec4 = ColorUtils.toVector4f(color);
    private Vector4f borderColorVec4 = ColorUtils.toVector4f(borderColor);

    public static SDFRectTexture of(int color) {
        return new SDFRectTexture().setColor(color);
    }

    @Override
    @ConfigSetter(field = "color")
    public SDFRectTexture setColor(int color) {
        this.color = color;
        this.colorVec4 = ColorUtils.toVector4f(color);
        return this;
    }

    @ConfigSetter(field = "borderColor")
    public SDFRectTexture setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        this.borderColorVec4 = ColorUtils.toVector4f(borderColor);
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
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        drawInternalWithColorVecs(graphics, x, y, width, height, colorVec4, borderColorVec4);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GUIContext context, float x, float y, float width, float height) {
        if (context.elementColor == -1) {
            drawInternalWithColorVecs(context.graphics, x, y, width, height, colorVec4, borderColorVec4);
        } else {
            var blendedFill   = ColorUtils.toVector4f(ColorUtils.mulColor(color,       context.elementColor));
            var blendedBorder = ColorUtils.toVector4f(ColorUtils.mulColor(borderColor, context.elementColor));
            drawInternalWithColorVecs(context.graphics, x, y, width, height, blendedFill, blendedBorder);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawInternalWithColorVecs(GuiGraphics graphics, float x, float y, float width, float height,
                                           Vector4f fillVec, Vector4f borderVec) {
        graphics.flush();
        // TODO calculate shape

        var halfWidth = width / 2f;
        var halfHeight = height / 2f;

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + halfWidth, y + halfHeight, 0);
        var mat = pose.last().pose();

        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(mat);
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.disableDepthTest();

        RenderSystem.setShader(LDLibShaders::getSDFRect);
        var shader = LDLibShaders.getSDFRect();
        shader.safeGetUniform("Radius").set(radius);
        shader.safeGetUniform("HalfSize").set(halfWidth, halfHeight);
        shader.safeGetUniform("FillColor").set(fillVec);

        shader.safeGetUniform("Border").set(stroke);

        if (stroke > 0) {
            shader.safeGetUniform("BorderColor").set(borderVec);
        }

        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION);

        buffer.addVertex(-halfWidth, halfHeight, 0);
        buffer.addVertex(halfWidth, halfHeight, 0);
        buffer.addVertex(halfWidth, -halfHeight, 0);
        buffer.addVertex(-halfWidth, -halfHeight, 0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        pose.popPose();
    }
}
