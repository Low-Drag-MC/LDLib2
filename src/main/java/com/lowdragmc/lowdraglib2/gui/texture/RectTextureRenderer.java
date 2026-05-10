package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class RectTextureRenderer {
    private RectTextureRenderer() {
    }

    public static void draw(RectTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, RectTextureRenderer::drawInternal);
    }

    private static void drawInternal(RectTexture texture, GUIContext context, float x, float y, float width, float height) {
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
