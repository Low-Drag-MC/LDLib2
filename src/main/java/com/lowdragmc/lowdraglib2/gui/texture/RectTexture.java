package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

@KJSBindings
@LDLRegisterClient(name = "rect_texture", registry = "ldlib2:gui_texture")
@Accessors(chain = true)
public class RectTexture extends TransformTexture {
    @Getter
    @Configurable
    @ConfigNumber(range = {0f, Float.MAX_VALUE}, wheel = 1)
    private Vector4f radius = new Vector4f(0, 0, 0, 0);
    @Getter
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
    @Getter
    @Configurable
    @ConfigNumber(range = {4, 32}, wheel = 1)
    private int cornerSegments = 8;
    
    private List<Vector2f>[] cachedCornerArcs = null;
    private boolean cachedSegments = false;

    public static RectTexture of(int color) {
        return new RectTexture().setColor(color);
    }
    
    @ConfigSetter(field = "radius")
    public RectTexture setRadius(Vector4f radius) {
        this.radius = radius;
//        this.cachedSegments = false;
        return this;
    }

    @ConfigSetter(field = "stroke")
    public RectTexture setStroke(float stroke) {
        this.stroke = stroke;
//        this.cachedSegments = false;
        return this;
    }

    @Override
    @ConfigSetter(field = "color")
    public RectTexture setColor(int color) {
        this.color = color;
//        this.colorVec4 = ColorUtils.toVector4f(color);
        return this;
    }

    @ConfigSetter(field = "borderColor")
    public RectTexture setBorderColor(int borderColor) {
        this.borderColor = borderColor;
//        this.borderColorVec4 = ColorUtils.toVector4f(borderColor);
        return this;
    }

    @ConfigSetter(field = "cornerSegments")
    public RectTexture setCornerSegments(int cornerSegments) {
        this.cornerSegments = cornerSegments;
        this.cachedSegments = false;
        return this;
    }

    @Override
    public RectTexture copy() {
        var copied = new RectTexture();
        copied.setRadius(new Vector4f(radius));
        copied.setStroke(stroke);
        copied.setColor(color);
        copied.setBorderColor(borderColor);
        copied.setCornerSegments(cornerSegments);
        copied.cachedSegments = cachedSegments;
        copied.copyTransform(this);
        return copied;
    }

    @Override
    public IGuiTexture interpolate(IGuiTexture other, float lerp) {
        if (other.getRawTexture() instanceof RectTexture rect) {
            var blended = new RectTexture();
            blended.setRadius(new Vector4f(radius).lerp(rect.getRadius(), lerp));
            blended.setStroke((1 - lerp) * stroke + rect.stroke * lerp);
            blended.setColor(ColorUtils.blendOklabColor(color, rect.color, lerp));
            blended.setBorderColor(ColorUtils.blendOklabColor(borderColor, rect.borderColor, lerp));
            blended.setCornerSegments(cornerSegments);
            blended.copyTransform(Transform2D.interpolate(getTransform2D(), rect.getTransform2D(), lerp));
            return blended;
        }
        return super.interpolate(other, lerp);
    }
    
    @SuppressWarnings("unchecked")
    private void ensureCornerCache() {
        if (cachedCornerArcs != null && cachedSegments) {
            return;
        }
        
        cachedSegments = true;
        cachedCornerArcs = new List[4];
        
        double[][] angleRanges = {
            {Math.PI, Math.PI * 1.5},       // 左上
            {Math.PI * 1.5, Math.PI * 2},   // 右上
            {0, Math.PI * 0.5},             // 右下
            {Math.PI * 0.5, Math.PI}        // 左下
        };
        
        for (int corner = 0; corner < 4; corner++) {
            cachedCornerArcs[corner] = new ArrayList<>(cornerSegments + 1);
            double startAngle = angleRanges[corner][0];
            double endAngle = angleRanges[corner][1];
            double step = (endAngle - startAngle) / cornerSegments;
            
            for (int i = 0; i <= cornerSegments; i++) {
                double angle = startAngle + i * step;
                cachedCornerArcs[corner].add(new Vector2f((float) Math.cos(angle), (float) Math.sin(angle)));
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        ensureCornerCache();

        var mat = graphics.pose().last().pose();

        var buffer = graphics.bufferSource().getBuffer(LDLibRenderTypes.rect());
        RenderSystem.disableDepthTest();

        if (ColorUtils.alpha(color) > 0) {
            drawFill(buffer, mat, x, y, width, height);
        }
        
        if (stroke > 0 && ColorUtils.alpha(borderColor) > 0) {
            drawBorder(buffer, mat, x, y, width, height);
        }
    }
    
    private void drawFill(VertexConsumer buffer, Matrix4f mat, float x, float y, float width, float height) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        float maxRadiusX = width / 2f;
        float maxRadiusY = height / 2f;
        float r0 = Math.min(radius.w, Math.min(maxRadiusX, maxRadiusY)); // 左上
        float r1 = Math.min(radius.z, Math.min(maxRadiusX, maxRadiusY)); // 右上
        float r2 = Math.min(radius.y, Math.min(maxRadiusX, maxRadiusY)); // 右下
        float r3 = Math.min(radius.x, Math.min(maxRadiusX, maxRadiusY)); // 左下

        // center
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;

        // outline
        List<float[]> outlineVertices = new ArrayList<>();

        // left top
        float cx0 = x + r0;
        float cy0 = y + r0;
        for (Vector2f v : cachedCornerArcs[0]) {
            outlineVertices.add(new float[]{cx0 + v.x * r0, cy0 + v.y * r0});
        }

        // right top
        float cx1 = x + width - r1;
        float cy1 = y + r1;
        for (Vector2f v : cachedCornerArcs[1]) {
            outlineVertices.add(new float[]{cx1 + v.x * r1, cy1 + v.y * r1});
        }

        // right bottom
        float cx2 = x + width - r2;
        float cy2 = y + height - r2;
        for (Vector2f v : cachedCornerArcs[2]) {
            outlineVertices.add(new float[]{cx2 + v.x * r2, cy2 + v.y * r2});
        }

        // left bottom
        float cx3 = x + r3;
        float cy3 = y + height - r3;
        for (Vector2f v : cachedCornerArcs[3]) {
            outlineVertices.add(new float[]{cx3 + v.x * r3, cy3 + v.y * r3});
        }

        int vertexCount = outlineVertices.size();
        for (int i = 0; i < vertexCount; i++) {
            float[] v1 = outlineVertices.get(i);
            float[] v2 = outlineVertices.get((i + 1) % vertexCount);

            buffer.addVertex(mat, v2[0], v2[1], 0).setColor(r, g, b, a);
            buffer.addVertex(mat, v1[0], v1[1], 0).setColor(r, g, b, a);
            buffer.addVertex(mat, centerX, centerY, 0).setColor(r, g, b, a);
        }
    }
    
    private void drawBorder(VertexConsumer buffer, Matrix4f mat, float x, float y, float width, float height) {
        int r = (borderColor >> 16) & 0xFF;
        int g = (borderColor >> 8) & 0xFF;
        int b = borderColor & 0xFF;
        int a = (borderColor >> 24) & 0xFF;

        float maxRadiusX = width / 2f;
        float maxRadiusY = height / 2f;
        float r0 = Math.min(radius.w, Math.min(maxRadiusX, maxRadiusY));
        float r1 = Math.min(radius.z, Math.min(maxRadiusX, maxRadiusY));
        float r2 = Math.min(radius.y, Math.min(maxRadiusX, maxRadiusY));
        float r3 = Math.min(radius.x, Math.min(maxRadiusX, maxRadiusY));

        // inner radius
        float ir0 = Math.max(r0 - stroke, 0), or0 = r0;
        float ir1 = Math.max(r1 - stroke, 0), or1 = r1;
        float ir2 = Math.max(r2 - stroke, 0), or2 = r2;
        float ir3 = Math.max(r3 - stroke, 0), or3 = r3;

        // corners
        float cx0 = x + r0, cy0 = y + r0;
        float cx1 = x + width - r1, cy1 = y + r1;
        float cx2 = x + width - r2, cy2 = y + height - r2;
        float cx3 = x + r3, cy3 = y + height - r3;

        // stroke
        float icx0 = x + stroke + ir0, icy0 = y + stroke + ir0;
        float icx1 = x + width - stroke - ir1, icy1 = y + stroke + ir1;
        float icx2 = x + width - stroke - ir2, icy2 = y + height - stroke - ir2;
        float icx3 = x + stroke + ir3, icy3 = y + height - stroke - ir3;

        // vertices
        List<float[]> outerVertices = new ArrayList<>();
        List<float[]> innerVertices = new ArrayList<>();

        for (Vector2f v : cachedCornerArcs[0]) {
            outerVertices.add(new float[]{cx0 + v.x * or0, cy0 + v.y * or0});
            innerVertices.add(new float[]{icx0 + v.x * ir0, icy0 + v.y * ir0});
        }

        for (Vector2f v : cachedCornerArcs[1]) {
            outerVertices.add(new float[]{cx1 + v.x * or1, cy1 + v.y * or1});
            innerVertices.add(new float[]{icx1 + v.x * ir1, icy1 + v.y * ir1});
        }

        for (Vector2f v : cachedCornerArcs[2]) {
            outerVertices.add(new float[]{cx2 + v.x * or2, cy2 + v.y * or2});
            innerVertices.add(new float[]{icx2 + v.x * ir2, icy2 + v.y * ir2});
        }

        for (Vector2f v : cachedCornerArcs[3]) {
            outerVertices.add(new float[]{cx3 + v.x * or3, cy3 + v.y * or3});
            innerVertices.add(new float[]{icx3 + v.x * ir3, icy3 + v.y * ir3});
        }

        int vertexCount = outerVertices.size();
        for (int i = 0; i < vertexCount; i++) {
            int next = (i + 1) % vertexCount;

            float[] o1 = outerVertices.get(i);
            float[] i1 = innerVertices.get(i);
            float[] o2 = outerVertices.get(next);
            float[] i2 = innerVertices.get(next);

            buffer.addVertex(mat, o1[0], o1[1], 0).setColor(r, g, b, a);
            buffer.addVertex(mat, i1[0], i1[1], 0).setColor(r, g, b, a);
            buffer.addVertex(mat, o2[0], o2[1], 0).setColor(r, g, b, a);

            buffer.addVertex(mat, o2[0], o2[1], 0).setColor(r, g, b, a);
            buffer.addVertex(mat, i1[0], i1[1], 0).setColor(r, g, b, a);
            buffer.addVertex(mat, i2[0], i2[1], 0).setColor(r, g, b, a);
        }
    }
}
