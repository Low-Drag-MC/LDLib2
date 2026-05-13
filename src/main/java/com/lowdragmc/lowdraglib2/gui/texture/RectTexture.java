package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import lombok.Getter;
import lombok.experimental.Accessors;
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

    List<Vector2f>[] cornerArcs() {
        ensureCornerCache();
        return cachedCornerArcs;
    }

    @LDLRegisterClient(name = "rect_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredRectTextureRenderer implements RegisteredGuiTextureRenderer<RectTexture, RegisteredRectTextureRenderer> {
        @Override
        public Class<RectTexture> type() {
            return RectTexture.class;
        }

        @Override
        public void draw(RectTexture texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(RectTexture texture, GUIContext context, float x, float y, float width, float height) {
            if (ColorUtils.alpha(texture.getColor()) > 0) {
                drawFill(texture, context, x, y, width, height);
            }
            if (texture.getStroke() > 0 && ColorUtils.alpha(texture.getBorderColor()) > 0) {
                drawBorder(texture, context, x, y, width, height);
            }
        }

        private static void drawFill(RectTexture texture, GUIContext context, float x, float y, float width, float height) {
            float maxRadiusX = width / 2f;
            float maxRadiusY = height / 2f;
            var radius = texture.getRadius();
            float r0 = Math.min(radius.x, Math.min(maxRadiusX, maxRadiusY));
            float r1 = Math.min(radius.y, Math.min(maxRadiusX, maxRadiusY));
            float r2 = Math.min(radius.z, Math.min(maxRadiusX, maxRadiusY));
            float r3 = Math.min(radius.w, Math.min(maxRadiusX, maxRadiusY));

            Vector2f center = new Vector2f(x + width / 2f, y + height / 2f);
            List<Vector2f> outlineVertices = new ArrayList<>();
            var cornerArcs = texture.cornerArcs();

            float cx0 = x + r0;
            float cy0 = y + r0;
            for (Vector2f v : cornerArcs[0]) {
                outlineVertices.add(new Vector2f(cx0 + v.x * r0, cy0 + v.y * r0));
            }

            float cx1 = x + width - r1;
            float cy1 = y + r1;
            for (Vector2f v : cornerArcs[1]) {
                outlineVertices.add(new Vector2f(cx1 + v.x * r1, cy1 + v.y * r1));
            }

            float cx2 = x + width - r2;
            float cy2 = y + height - r2;
            for (Vector2f v : cornerArcs[2]) {
                outlineVertices.add(new Vector2f(cx2 + v.x * r2, cy2 + v.y * r2));
            }

            float cx3 = x + r3;
            float cy3 = y + height - r3;
            for (Vector2f v : cornerArcs[3]) {
                outlineVertices.add(new Vector2f(cx3 + v.x * r3, cy3 + v.y * r3));
            }

            int vertexCount = outlineVertices.size();
            for (int i = 0; i < vertexCount; i++) {
                Vector2f v1 = outlineVertices.get(i);
                Vector2f v2 = outlineVertices.get((i + 1) % vertexCount);
                context.fillTriangle(LDLibRenderPipelines.GUI_TRIANGLE, center, v2, v1, texture.getColor());
            }
        }

        private static void drawBorder(RectTexture texture, GUIContext context, float x, float y, float width, float height) {
            float maxRadiusX = width / 2f;
            float maxRadiusY = height / 2f;
            var radius = texture.getRadius();
            float stroke = texture.getStroke();
            float r0 = Math.min(radius.x, Math.min(maxRadiusX, maxRadiusY));
            float r1 = Math.min(radius.y, Math.min(maxRadiusX, maxRadiusY));
            float r2 = Math.min(radius.z, Math.min(maxRadiusX, maxRadiusY));
            float r3 = Math.min(radius.w, Math.min(maxRadiusX, maxRadiusY));

            float ir0 = Math.max(r0 - stroke, 0), or0 = r0;
            float ir1 = Math.max(r1 - stroke, 0), or1 = r1;
            float ir2 = Math.max(r2 - stroke, 0), or2 = r2;
            float ir3 = Math.max(r3 - stroke, 0), or3 = r3;

            float cx0 = x + r0, cy0 = y + r0;
            float cx1 = x + width - r1, cy1 = y + r1;
            float cx2 = x + width - r2, cy2 = y + height - r2;
            float cx3 = x + r3, cy3 = y + height - r3;

            float icx0 = x + stroke + ir0, icy0 = y + stroke + ir0;
            float icx1 = x + width - stroke - ir1, icy1 = y + stroke + ir1;
            float icx2 = x + width - stroke - ir2, icy2 = y + height - stroke - ir2;
            float icx3 = x + stroke + ir3, icy3 = y + height - stroke - ir3;

            List<Vector2f> outerVertices = new ArrayList<>();
            List<Vector2f> innerVertices = new ArrayList<>();
            var cornerArcs = texture.cornerArcs();

            for (Vector2f v : cornerArcs[0]) {
                outerVertices.add(new Vector2f(cx0 + v.x * or0, cy0 + v.y * or0));
                innerVertices.add(new Vector2f(icx0 + v.x * ir0, icy0 + v.y * ir0));
            }
            for (Vector2f v : cornerArcs[1]) {
                outerVertices.add(new Vector2f(cx1 + v.x * or1, cy1 + v.y * or1));
                innerVertices.add(new Vector2f(icx1 + v.x * ir1, icy1 + v.y * ir1));
            }
            for (Vector2f v : cornerArcs[2]) {
                outerVertices.add(new Vector2f(cx2 + v.x * or2, cy2 + v.y * or2));
                innerVertices.add(new Vector2f(icx2 + v.x * ir2, icy2 + v.y * ir2));
            }
            for (Vector2f v : cornerArcs[3]) {
                outerVertices.add(new Vector2f(cx3 + v.x * or3, cy3 + v.y * or3));
                innerVertices.add(new Vector2f(icx3 + v.x * ir3, icy3 + v.y * ir3));
            }

            int vertexCount = outerVertices.size();
            for (int i = 0; i < vertexCount; i++) {
                int next = (i + 1) % vertexCount;
                Vector2f o1 = outerVertices.get(i);
                Vector2f i1 = innerVertices.get(i);
                Vector2f o2 = outerVertices.get(next);
                Vector2f i2 = innerVertices.get(next);
                context.fillTriangle(LDLibRenderPipelines.GUI_TRIANGLE, o1, i1, o2, texture.getBorderColor());
                context.fillTriangle(LDLibRenderPipelines.GUI_TRIANGLE, o2, i1, i2, texture.getBorderColor());
            }
        }
    }
}
