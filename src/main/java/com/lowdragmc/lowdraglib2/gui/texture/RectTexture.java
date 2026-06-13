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
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.Vector2f;
import org.joml.Vector4f;

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

    private float[] cachedCornerArcXY = null;
    private int cachedCornerPoints = 0;
    private int cachedCornerStride = 0;
    private float[] outlineScratch = new float[0];
    private float[] outerScratch = new float[0];
    private float[] innerScratch = new float[0];
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

    private void ensureCornerCache() {
        if (cachedCornerArcXY != null && cachedSegments) {
            return;
        }

        cachedSegments = true;
        cachedCornerPoints = cornerSegments + 1;
        cachedCornerStride = cachedCornerPoints * 2;
        cachedCornerArcXY = new float[4 * cachedCornerStride];

        double[][] angleRanges = {
                {Math.PI, Math.PI * 1.5},
                {Math.PI * 1.5, Math.PI * 2},
                {0, Math.PI * 0.5},
                {Math.PI * 0.5, Math.PI}
        };

        for (int corner = 0; corner < 4; corner++) {
            double startAngle = angleRanges[corner][0];
            double endAngle = angleRanges[corner][1];
            double step = (endAngle - startAngle) / cornerSegments;
            int cornerBase = corner * cachedCornerStride;

            for (int i = 0; i <= cornerSegments; i++) {
                double angle = startAngle + i * step;
                int index = cornerBase + i * 2;
                cachedCornerArcXY[index] = (float) Math.cos(angle);
                cachedCornerArcXY[index + 1] = (float) Math.sin(angle);
            }
        }
    }

    private float[] ensureScratchCapacity(float[] scratch, int required) {
        return scratch.length >= required ? scratch : new float[required];
    }

    boolean canUsePlainRectPath(float width, float height) {
        float maxRadius = Math.min(width * 0.5f, height * 0.5f);
        return Math.min(radius.x, maxRadius) <= 0
                && Math.min(radius.y, maxRadius) <= 0
                && Math.min(radius.z, maxRadius) <= 0
                && Math.min(radius.w, maxRadius) <= 0;
    }

    private void appendCorner(float[] vertices, int vertexOffset, int corner, float centerX, float centerY, float radius) {
        int sourceBase = corner * cachedCornerStride;
        int targetBase = vertexOffset * 2;
        for (int i = 0; i < cachedCornerPoints; i++) {
            int sourceIndex = sourceBase + i * 2;
            int targetIndex = targetBase + i * 2;
            vertices[targetIndex] = centerX + cachedCornerArcXY[sourceIndex] * radius;
            vertices[targetIndex + 1] = centerY + cachedCornerArcXY[sourceIndex + 1] * radius;
        }
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
            if (width <= 0 || height <= 0) {
                return;
            }
            if (texture.canUsePlainRectPath(width, height)) {
                drawPlainRect(texture, context, x, y, width, height);
                return;
            }

            texture.ensureCornerCache();

            if (ColorUtils.alpha(texture.getColor()) > 0) {
                drawFill(texture, context, x, y, width, height);
            }
            if (texture.getStroke() > 0 && ColorUtils.alpha(texture.getBorderColor()) > 0) {
                drawBorder(texture, context, x, y, width, height);
            }
        }

        private static void drawPlainRect(RectTexture texture, GUIContext context, float x, float y, float width, float height) {
            if (ColorUtils.alpha(texture.getColor()) > 0) {
                DrawerHelperClient.drawSolidRect(context, x, y, width, height, texture.getColor());
            }

            if (texture.getStroke() > 0 && ColorUtils.alpha(texture.getBorderColor()) > 0) {
                float border = Math.min(texture.getStroke(), Math.min(width * 0.5f, height * 0.5f));
                DrawerHelperClient.drawSolidRect(context, x, y, width, border, texture.getBorderColor());
                DrawerHelperClient.drawSolidRect(context, x, y + height - border, width, border, texture.getBorderColor());
                DrawerHelperClient.drawSolidRect(context, x, y + border, border, height - border * 2, texture.getBorderColor());
                DrawerHelperClient.drawSolidRect(context, x + width - border, y + border, border, height - border * 2, texture.getBorderColor());
            }
        }

        private static void drawFill(RectTexture texture, GUIContext context, float x, float y, float width, float height) {
            float halfWidth = width * 0.5f;
            float halfHeight = height * 0.5f;
            float maxRadius = Math.min(halfWidth, halfHeight);
            var radius = texture.getRadius();
            float r0 = Math.min(radius.x, maxRadius);
            float r1 = Math.min(radius.y, maxRadius);
            float r2 = Math.min(radius.z, maxRadius);
            float r3 = Math.min(radius.w, maxRadius);

            float centerX = x + halfWidth;
            float centerY = y + halfHeight;
            int vertexCount = texture.cachedCornerPoints * 4;

            texture.outlineScratch = texture.ensureScratchCapacity(texture.outlineScratch, vertexCount * 2);

            texture.appendCorner(texture.outlineScratch, 0, 0, x + r0, y + r0, r0);
            texture.appendCorner(texture.outlineScratch, texture.cachedCornerPoints, 1, x + width - r1, y + r1, r1);
            texture.appendCorner(texture.outlineScratch, texture.cachedCornerPoints * 2, 2, x + width - r2, y + height - r2, r2);
            texture.appendCorner(texture.outlineScratch, texture.cachedCornerPoints * 3, 3, x + r3, y + height - r3, r3);

            for (int i = 0; i < vertexCount; i++) {
                int current = i * 2;
                int next = ((i + 1) % vertexCount) * 2;

                fillTriangle(context,
                        texture.outlineScratch[next], texture.outlineScratch[next + 1],
                        texture.outlineScratch[current], texture.outlineScratch[current + 1],
                        centerX, centerY,
                        texture.getColor());
            }
        }

        private static void drawBorder(RectTexture texture, GUIContext context, float x, float y, float width, float height) {
            float halfWidth = width * 0.5f;
            float halfHeight = height * 0.5f;
            float maxRadius = Math.min(halfWidth, halfHeight);
            var radius = texture.getRadius();
            float stroke = texture.getStroke();
            float r0 = Math.min(radius.x, maxRadius);
            float r1 = Math.min(radius.y, maxRadius);
            float r2 = Math.min(radius.z, maxRadius);
            float r3 = Math.min(radius.w, maxRadius);

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

            int vertexCount = texture.cachedCornerPoints * 4;
            texture.outerScratch = texture.ensureScratchCapacity(texture.outerScratch, vertexCount * 2);
            texture.innerScratch = texture.ensureScratchCapacity(texture.innerScratch, vertexCount * 2);

            texture.appendCorner(texture.outerScratch, 0, 0, cx0, cy0, or0);
            texture.appendCorner(texture.innerScratch, 0, 0, icx0, icy0, ir0);
            texture.appendCorner(texture.outerScratch, texture.cachedCornerPoints, 1, cx1, cy1, or1);
            texture.appendCorner(texture.innerScratch, texture.cachedCornerPoints, 1, icx1, icy1, ir1);
            texture.appendCorner(texture.outerScratch, texture.cachedCornerPoints * 2, 2, cx2, cy2, or2);
            texture.appendCorner(texture.innerScratch, texture.cachedCornerPoints * 2, 2, icx2, icy2, ir2);
            texture.appendCorner(texture.outerScratch, texture.cachedCornerPoints * 3, 3, cx3, cy3, or3);
            texture.appendCorner(texture.innerScratch, texture.cachedCornerPoints * 3, 3, icx3, icy3, ir3);

            for (int i = 0; i < vertexCount; i++) {
                int current = i * 2;
                int next = ((i + 1) % vertexCount) * 2;

                fillTriangle(context,
                        texture.outerScratch[current], texture.outerScratch[current + 1],
                        texture.innerScratch[current], texture.innerScratch[current + 1],
                        texture.outerScratch[next], texture.outerScratch[next + 1],
                        texture.getBorderColor());
                fillTriangle(context,
                        texture.outerScratch[next], texture.outerScratch[next + 1],
                        texture.innerScratch[current], texture.innerScratch[current + 1],
                        texture.innerScratch[next], texture.innerScratch[next + 1],
                        texture.getBorderColor());
            }
        }

        private static void fillTriangle(
                GUIContext context,
                float x0, float y0,
                float x1, float y1,
                float x2, float y2,
                int color
        ) {
            context.fillTriangle(
                    LDLibRenderPipelines.GUI_TRIANGLE,
                    new Vector2f(x0, y0),
                    new Vector2f(x1, y1),
                    new Vector2f(x2, y2),
                    color
            );
        }
    }
}
