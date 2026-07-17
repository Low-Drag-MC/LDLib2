package com.lowdragmc.lowdraglib2.client.utils;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import oshi.util.tuples.Pair;

import javax.annotation.Nonnull;
import java.util.List;

public class RenderBufferUtils {

    public static void drawLine(PoseStack.Pose pose, VertexConsumer buffer, Vector3f from, Vector3f to,
                                float sr, float sg, float sb, float sa, float er, float eg, float eb, float ea, float sW, float eW) {
        var normalDir = new Vector3f(to.x - from.x, to.y - from.y, to.z - from.z).normalize();
        // 26.1's LINES_SNIPPET uses POSITION_COLOR_NORMAL_LINE_WIDTH; LineWidth must be present
        // per vertex or BufferBuilder.endLastVertex throws "Missing elements in vertex: LineWidth".
        buffer.addVertex(pose, from.x, from.y, from.z).setColor(sr, sg, sb, sa)
                .setNormal(pose, normalDir.x, normalDir.y, normalDir.z).setLineWidth(sW);
        buffer.addVertex(pose, to.x, to.y, to.z).setColor(er, eg, eb, ea)
                .setNormal(pose, normalDir.x, normalDir.y, normalDir.z).setLineWidth(eW);
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endLastBatch();
        }
    }

    public static void drawLine(Matrix4f pose, VertexConsumer buffer, Vector3f from, Vector3f to,
                                float sr, float sg, float sb, float sa, float er, float eg, float eb, float ea, float sW, float eW) {
        var normalDir = new Vector3f(to.x - from.x, to.y - from.y, to.z - from.z).normalize();
        normalDir = pose.transformDirection(normalDir);
        buffer.addVertex(pose, from.x, from.y, from.z).setColor(sr, sg, sb, sa)
                .setNormal(normalDir.x, normalDir.y, normalDir.z).setLineWidth(sW);
        buffer.addVertex(pose, to.x, to.y, to.z).setColor(er, eg, eb, ea)
                .setNormal(normalDir.x, normalDir.y, normalDir.z).setLineWidth(eW);
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endLastBatch();
        }
    }

    public static void drawLines(PoseStack poseStack, VertexConsumer buffer, List<Vector3f> points, int colorStart, int colorEnd, float widthStart, float widthEnd) {
        if (points.size() < 2) return;
        Vector3f lastPoint = points.getFirst();
        Vector3f point;
        int sa = (colorStart >> 24) & 0xff, sr = (colorStart >> 16) & 0xff, sg = (colorStart >> 8) & 0xff, sb = colorStart & 0xff;
        int ea = (colorEnd >> 24) & 0xff, er = (colorEnd >> 16) & 0xff, eg = (colorEnd >> 8) & 0xff, eb = colorEnd & 0xff;
        ea = (ea - sa);
        er = (er - sr);
        eg = (eg - sg);
        eb = (eb - sb);
        for (int i = 1; i < points.size(); i++) {
            float s = (i - 1f) / points.size();
            float e = i * 1f / points.size();
            point = points.get(i);
            drawLine(poseStack.last().pose(), buffer, lastPoint, point, (sr + er * s) / 255, (sg + eg * s) / 255, (sb + eb * s) / 255, (sa + ea * s) / 255,
                    (sr + er * e) / 255, (sg + eg * e) / 255, (sb + eb * e) / 255, (sa + ea * e) / 255, widthStart, widthEnd);
        }
    }

    public static void drawCubeFrame(PoseStack poseStack, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a, float width) {
        var mat = poseStack.last().pose();
        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a).setNormal(1,0,0).setLineWidth(width);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a).setNormal(1,0,0).setLineWidth(width);

        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a).setNormal(0,1,0).setLineWidth(width);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a).setNormal(0,1,0).setLineWidth(width);

        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a).setNormal(0,0,1).setLineWidth(width);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a).setNormal(0,0,1).setLineWidth(width);

        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(1,0,0).setLineWidth(width);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(1,0,0).setLineWidth(width);

        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(0,1,0).setLineWidth(width);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(0,1,0).setLineWidth(width);

        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(0,0,1).setLineWidth(width);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(0,0,1).setLineWidth(width);

        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a).setNormal(0,0,1).setLineWidth(width);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(0,0,1).setLineWidth(width);

        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a).setNormal(1,0,0).setLineWidth(width);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(1,0,0).setLineWidth(width);

        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a).setNormal(0,0,1).setLineWidth(width);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(0,0,1).setLineWidth(width);

        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a).setNormal(0,1,0).setLineWidth(width);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(0,1,0).setLineWidth(width);

        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a).setNormal(1,0,0).setLineWidth(width);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(1,0,0).setLineWidth(width);

        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a).setNormal(0,1,0).setLineWidth(width);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(0,1,0).setLineWidth(width);
    }

    public static void drawCubeFace(PoseStack poseStack, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float a, boolean shade) {
        Matrix4f mat = poseStack.last().pose();
        float r = red, g = green, b = blue;

        if (minZ != maxZ && minY != maxY) {
            if (shade) {
                r *= 0.6;
                g *= 0.6;
                b *= 0.6;
            }

            buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a);

            buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);

            buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);

            buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a);
        }


        if (minX != maxX && minZ != maxZ ) {
            if (shade) {
                r = red * 0.5f;
                g = green * 0.5f;
                b = blue * 0.5f;
            }
            buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a);

            buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);

            if (shade) {
                r = red;
                g = green;
                b = blue;
            }
            buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);

            buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a);

        }


        if (minX != maxX && minY != maxY) {
            if (shade) {
                r = red * 0.8f;
                g = green * 0.8f;
                b = blue * 0.8f;
            }
            buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a);

            buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);

            buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);

            buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a);
            buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a);
        }
    }

    public static void renderCubeFace(PoseStack poseStack, BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float a, boolean shade) {
        Matrix4f mat = poseStack.last().pose();
        float r = red, g = green, b = blue;

        if (shade) {
            r *= 0.6;
            g *= 0.6;
            b *= 0.6;
        }
        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a);

        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a);

        if (shade) {
            r = red * 0.5f;
            g = green * 0.5f;
            b = blue * 0.5f;
        }
        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a);

        if (shade) {
            r = red;
            g = green;
            b = blue;
        }
        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a);

        if (shade) {
            r = red * 0.8f;
            g = green * 0.8f;
            b = blue * 0.8f;
        }
        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a);

        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a);
    }

    public static void renderCubeFace(PoseStack poseStack, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color, int combinedLight, TextureAtlasSprite textureSprite) {
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose normal = poseStack.last();
        float uMin = textureSprite.getU0();
        float uMax = textureSprite.getU1();
        float vMin = textureSprite.getV0();
        float vMax = textureSprite.getV1();

        buffer.addVertex(mat, minX, minY, minZ).setColor(color).setUv(uMin, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, -1, 0, 0);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(color).setUv(uMax, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, -1, 0, 0);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(color).setUv(uMax, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, -1, 0, 0);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(color).setUv(uMin, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, -1, 0, 0);

        buffer.addVertex(mat, maxX, minY, minZ).setColor(color).setUv(uMin, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 1, 0, 0);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(color).setUv(uMax, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 1, 0, 0);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(color).setUv(uMax, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 1, 0, 0);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(color).setUv(uMin, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 1, 0, 0);


        buffer.addVertex(mat, minX, minY, minZ).setColor(color).setUv(uMin, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, -1, 0);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(color).setUv(uMax, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, -1, 0);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(color).setUv(uMax, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, -1, 0);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(color).setUv(uMin, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, -1, 0);


        buffer.addVertex(mat, minX, maxY, minZ).setColor(color).setUv(uMin, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 1, 0);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(color).setUv(uMax, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 1, 0);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(color).setUv(uMax, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 1, 0);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(color).setUv(uMin, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 1, 0);

        buffer.addVertex(mat, minX, minY, minZ).setColor(color).setUv(uMin, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 0, -1);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(color).setUv(uMax, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 0, -1);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(color).setUv(uMax, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 0, -1);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(color).setUv(uMin, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 0, -1);

        buffer.addVertex(mat, minX, minY, maxZ).setColor(color).setUv(uMin, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 0, 1);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(color).setUv(uMax, vMax).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 0, 1);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(color).setUv(uMax, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 0, 1);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(color).setUv(uMin, vMin).setOverlay(OverlayTexture.NO_OVERLAY).setLight(combinedLight).setNormal(normal, 0, 0, 1);
    }

    public static void drawEdges(@Nonnull PoseStack poseStack, VertexConsumer buffer, List<Pair<Vector3f, Vector3f>> lines, int color) {
        var pose = poseStack.last();
        var mat = pose.pose();

        for (var line : lines) {
            var a = line.getA();
            var b = line.getB();
            float f = b.x - a.x;
            float f1 = b.y - a.y;
            float f2 = b.z - a.z;
            float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
            f /= f3;
            f1 /= f3;
            f2 /= f3;

            buffer.addVertex(mat, a.x, a.y, a.z).setColor(color).setNormal(poseStack.last(), f, f1, f2);
            buffer.addVertex(mat, b.x, b.y, b.z ).setColor(color).setNormal(poseStack.last(), f, f1, f2);
        }
    }

    public static void drawColorLines(@Nonnull Matrix3x2fc pose, VertexConsumer builder, List<Vector2f> points, int colorStart, int colorEnd, float width) {
        drawColorLines(pose, builder, points, colorStart, colorEnd, width, true);
    }

    public static void drawColorLines(@Nonnull Matrix3x2fc pose,
                                      VertexConsumer builder,
                                      List<Vector2f> points,
                                      int colorStart, int colorEnd,
                                      float halfWidth,
                                      boolean stripSide) {
        int n = points.size();
        if (n < 2) return;

        int sa0 = (colorStart >>> 24) & 0xFF, sr0 = (colorStart >>> 16) & 0xFF, sg0 = (colorStart >>> 8) & 0xFF, sb0 = colorStart & 0xFF;
        int ea0 = (colorEnd >>> 24) & 0xFF, er0 = (colorEnd >>> 16) & 0xFF, eg0 = (colorEnd >>> 8) & 0xFF, eb0 = colorEnd & 0xFF;

        int da = ea0 - sa0, dr = er0 - sr0, dg = eg0 - sg0, db = eb0 - sb0;
        int segCount = n - 1;

        float invSegCount = 1f / segCount;
        float colorMul = 1f / 255f;

        // Each segment is emitted as an INDEPENDENT quad (its perpendicular is a fixed 90° rotation of the
        // segment direction, so every quad winds consistently), and consecutive quads are separated by
        // degenerate triangles. This prevents a sharp/near-vertical corner from folding the connected strip
        // into a self-intersecting "bowtie" — the old behavior shared each point's vertices between two
        // segments, so at a corner the perpendicular flipped and one triangle ended up wound the wrong way
        // for its strip parity and got back-face culled (the segment vanished).
        Vector2f prev = null;
        int prevIdx = 0;
        boolean first = true;
        float lastBx = 0, lastBy = 0; // previous quad's last vertex (curr - perp), for the degenerate join
        int i = 0;
        for (Vector2f cur : points) {
            if (prev == null) { prev = cur; prevIdx = i; i++; continue; }

            float dx = cur.x - prev.x;
            float dy = cur.y - prev.y;
            float len2 = dx * dx + dy * dy;
            if (len2 < 1.0e-12f) { prev = cur; prevIdx = i; i++; continue; } // collapse zero-length segments

            float invLenHalfW = (float) (1.0 / Math.sqrt(len2)) * halfWidth;
            float px = -dy * invLenHalfW;
            float py = dx * invLenHalfW;

            float t0 = prevIdx * invSegCount;
            float t1 = i * invSegCount;
            float r0 = (sr0 + dr * t0) * colorMul, g0 = (sg0 + dg * t0) * colorMul, b0 = (sb0 + db * t0) * colorMul, a0 = (sa0 + da * t0) * colorMul;
            float r1 = (sr0 + dr * t1) * colorMul, g1 = (sg0 + dg * t1) * colorMul, b1 = (sb0 + db * t1) * colorMul, a1 = (sa0 + da * t1) * colorMul;

            float aX = prev.x + px, aY = prev.y + py; // quad first vertex (prev + perp)
            float eX = cur.x - px,  eY = cur.y - py;  // quad last vertex (cur - perp)

            if (first) {
                // leading duplicate of the first vertex: shifts the strip parity by one so the real
                // triangles land on odd indices and come out front-facing (the GPU flips winding for
                // odd strip triangles). Without it every triangle is back-facing → all culled → nothing
                // renders. Each later degenerate join adds 2 verts, preserving this parity.
                builder.addVertexWith2DPose(pose, aX, aY).setColor(r0, g0, b0, a0);
            } else {
                // degenerate join: repeat the previous quad's last vertex and this quad's first vertex
                // (two zero-area triangles) so the two quads don't connect across the corner.
                builder.addVertexWith2DPose(pose, lastBx, lastBy).setColor(r0, g0, b0, a0);
                builder.addVertexWith2DPose(pose, aX, aY).setColor(r0, g0, b0, a0);
            }
            builder.addVertexWith2DPose(pose, aX, aY).setColor(r0, g0, b0, a0);                       // prev + perp
            builder.addVertexWith2DPose(pose, prev.x - px, prev.y - py).setColor(r0, g0, b0, a0);      // prev - perp
            builder.addVertexWith2DPose(pose, cur.x + px, cur.y + py).setColor(r1, g1, b1, a1);        // cur + perp
            builder.addVertexWith2DPose(pose, eX, eY).setColor(r1, g1, b1, a1);                        // cur - perp

            lastBx = eX; lastBy = eY;
            first = false;
            prev = cur; prevIdx = i; i++;
        }
    }

    public static void drawColorTexLines(@Nonnull Matrix3x2fc pose,
                                         VertexConsumer builder,
                                         List<Vector2f> points,
                                         int colorStart, int colorEnd,
                                         float halfWidth,
                                         boolean stripSide) {
        int n = points.size();
        if (n < 2) return;

        int sa0 = (colorStart >>> 24) & 0xFF, sr0 = (colorStart >>> 16) & 0xFF, sg0 = (colorStart >>> 8) & 0xFF, sb0 = colorStart & 0xFF;
        int ea0 = (colorEnd >>> 24) & 0xFF, er0 = (colorEnd >>> 16) & 0xFF, eg0 = (colorEnd >>> 8) & 0xFF, eb0 = colorEnd & 0xFF;

        int da = ea0 - sa0, dr = er0 - sr0, dg = eg0 - sg0, db = eb0 - sb0;
        int segCount = n - 1;

        var last = points.getFirst();
        Vector2f curr = null;
        Vector3f perp = new Vector3f();
        boolean emittedAny = false;

        for (int i = 1; i < n; i++) {
            float u = (i - 1f) / segCount;
            float t = u;

            float r = (sr0 + dr * t) / 255f;
            float g = (sg0 + dg * t) / 255f;
            float b = (sb0 + db * t) / 255f;
            float a = (sa0 + da * t) / 255f;

            curr = points.get(i);

            float dx = curr.x - last.x;
            float dy = curr.y - last.y;
            float len2 = dx * dx + dy * dy;
            if (len2 < 1.0e-12f) {
                last = curr;
                continue;
            }

            float invLen = (float) (1.0 / Math.sqrt(len2));
            perp.set(-dy * invLen * halfWidth, dx * invLen * halfWidth, 0);

            builder.addVertexWith2DPose(pose, last.x + perp.x, last.y + perp.y)
                    .setUv(u, 0)
                    .setColor(r, g, b, a);

            if (stripSide && !emittedAny) {
                builder.addVertexWith2DPose(pose, last.x + perp.x, last.y + perp.y)
                        .setUv(u, 0)
                        .setColor(r, g, b, a);
            }

            builder.addVertexWith2DPose(pose, last.x - perp.x, last.y - perp.y)
                    .setUv(u, 1)
                    .setColor(r, g, b, a);

            emittedAny = true;
            last = curr;
        }

        if (!emittedAny || curr == null) return;

        float rEnd = (sr0 + dr) / 255f;
        float gEnd = (sg0 + dg) / 255f;
        float bEnd = (sb0 + db) / 255f;
        float aEnd = (sa0 + da) / 255f;

        builder.addVertexWith2DPose(pose, curr.x + perp.x, curr.y + perp.y)
                .setUv(1, 0)
                .setColor(rEnd, gEnd, bEnd, aEnd);

        builder.addVertexWith2DPose(pose, curr.x - perp.x, curr.y - perp.y)
                .setUv(1, 1)
                .setColor(rEnd, gEnd, bEnd, aEnd);

        if (stripSide) {
            builder.addVertexWith2DPose(pose, curr.x - perp.x, curr.y - perp.y)
                    .setUv(1, 1)
                    .setColor(rEnd, gEnd, bEnd, aEnd);
        }
    }

    public static void drawCircleLine(@Nonnull PoseStack poseStack, VertexConsumer buffer,
                                      Vector3f position,
                                      Vector3f normal, int segments,
                                      float radius, float red, float green, float blue, float alpha, float lineWidth) {

        Matrix4f pose = poseStack.last().pose();

        if (segments < 3) {
            segments = 3;
        }

        Vector3f u = new Vector3f();
        Vector3f v = new Vector3f();

        if (normal.equals(new Vector3f(0, 0, 1))) {
            u.set(1, 0, 0);
            v.set(0, 1, 0);
        } else {
            if (Math.abs(normal.x) < Math.abs(normal.y) && Math.abs(normal.x) < Math.abs(normal.z)) {
                u.set(0, -normal.z, normal.y).normalize();
            } else if (Math.abs(normal.y) < Math.abs(normal.x) && Math.abs(normal.y) < Math.abs(normal.z)) {
                u.set(-normal.z, 0, normal.x).normalize();
            } else {
                u.set(-normal.y, normal.x, 0).normalize();
            }
            v.set(normal).cross(u).normalize();
            u.cross(normal, v).normalize();
        }

        Vector3f prevPoint = new Vector3f();
        Vector3f firstPoint = new Vector3f();

        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            float x = (float) (radius * Math.cos(angle));
            float y = (float) (radius * Math.sin(angle));

            Vector3f currentPoint = new Vector3f(position)
                    .add(u.x * x + v.x * y, u.y * x + v.y * y, u.z * x + v.z * y);

            if (i > 0) {
                drawLine(pose, buffer, prevPoint, currentPoint, red, green, blue, alpha, red, green, blue, alpha, lineWidth, lineWidth);
            } else {
                firstPoint.set(currentPoint);
            }

            prevPoint.set(currentPoint);
        }

        drawLine(pose, buffer, prevPoint, firstPoint, red, green, blue, alpha, red, green, blue, alpha, lineWidth, lineWidth);
    }

    /**
     *
     * cone
     *
     * @param poseStack  The stack used to store the transformation matrix.
     * @param buffer     Vertex consumer, which is used to cache vertex data.
     * @param x          The x coordinate of the center of the cone.
     * @param y          The y coordinate of the center of the cone.
     * @param z          The z coordinate of the center of the cone.
     * @param baseRadius The radius of the base of the cone.
     * @param height     The height of the cone.
     * @param segments   The number of subdivisions of the base.
     * @param red        color
     * @param green      color
     * @param blue       color
     * @param alpha      transparency
     * @param axis       The axial direction of the cone, which determines the direction of the cone.
     */
    public static void shapeCone(PoseStack poseStack, VertexConsumer buffer, float x, float y, float z, float baseRadius,
                                 float height, int segments, float red, float green, float blue, float alpha,
                                 Direction.Axis axis) {
        Matrix4f mat = poseStack.last().pose();
        float segmentDelta = (float) (2.0 * Math.PI / segments); // Subdivision angle of the base
        float theta = 0; // θ, sin(θ), cos(θ) Base angle
        float cosTheta = 1.0F;
        float sinTheta = 0.0F;

        float nextCosTheta, nextSinTheta;

        // Base vertices
        for (int i = 0; i < segments; i++) {
            float theta1 = theta + segmentDelta;
            nextCosTheta = Mth.cos(theta1);
            nextSinTheta = Mth.sin(theta1);

            switch (axis) {
                case Y -> {
                    // Base of the cone
                    buffer.addVertex(mat, x + cosTheta * baseRadius, y, z + sinTheta * baseRadius)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x + nextCosTheta * baseRadius, y, z + nextSinTheta * baseRadius)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x, y + height, z)
                            .setColor(red, green, blue, alpha);
                }
                case X -> {
                    buffer.addVertex(mat, x, y + cosTheta * baseRadius, z + sinTheta * baseRadius)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x, y + nextCosTheta * baseRadius, z + nextSinTheta * baseRadius)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x + height, y, z)
                            .setColor(red, green, blue, alpha);
                }
                case Z -> {
                    buffer.addVertex(mat, x + cosTheta * baseRadius, y + sinTheta * baseRadius, z)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x + nextCosTheta * baseRadius, y + nextSinTheta * baseRadius, z)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x, y, z + height)
                            .setColor(red, green, blue, alpha);
                }
            }

            theta = theta1;
            cosTheta = nextCosTheta;
            sinTheta = nextSinTheta;
        }
    }

    /**
     *
     * circle
     *
     * @param poseStack  The stack used to store the transformation matrix.
     * @param buffer     Vertex consumer, which is used to cache vertex data.
     * @param x          The x coordinate of the center of the cylinder.
     * @param y          The y coordinate of the center of the cylinder.
     * @param z          The z coordinate of the center of the cylinder.
     * @param baseRadius The radius of the base of the cylinder.
     * @param segments   The number of subdivisions of the base.
     * @param red        color
     * @param green      color
     * @param blue       color
     * @param alpha      transparency
     * @param axis       The axial direction of the cylinder, which determines the direction of the cylinder.
     */
    public static void shapeCircle(PoseStack poseStack, VertexConsumer buffer, float x, float y, float z, float baseRadius,
                                   int segments, float red, float green, float blue, float alpha,
                                   Direction.Axis axis) {
        Matrix4f mat = poseStack.last().pose();
        float segmentDelta = (float) (2.0 * Math.PI / segments); // Subdivision angle of the base
        float theta = 0; // θ, sin(θ), cos(θ) Base angle
        float cosTheta = 1.0F;
        float sinTheta = 0.0F;

        float nextCosTheta, nextSinTheta;

        // Base vertices
        for (int i = 0; i < segments; i++) {
            float theta1 = theta + segmentDelta;
            nextCosTheta = Mth.cos(theta1);
            nextSinTheta = Mth.sin(theta1);

            switch (axis) {
                case Y -> {
                    // Base disk
                    buffer.addVertex(mat, x, y, z)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x + nextCosTheta * baseRadius, y, z + nextSinTheta * baseRadius)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x + cosTheta * baseRadius, y, z + sinTheta * baseRadius)
                            .setColor(red, green, blue, alpha);
                }
                case X -> {
                    buffer.addVertex(mat, x, y, z)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x, y + nextCosTheta * baseRadius, z + nextSinTheta * baseRadius)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x, y + cosTheta * baseRadius, z + sinTheta * baseRadius)
                            .setColor(red, green, blue, alpha);
                }
                case Z -> {
                    buffer.addVertex(mat, x, y, z)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x + nextCosTheta * baseRadius, y + nextSinTheta * baseRadius, z)
                            .setColor(red, green, blue, alpha);
                    buffer.addVertex(mat, x + cosTheta * baseRadius, y + sinTheta * baseRadius, z)
                            .setColor(red, green, blue, alpha);
                }
            }

            theta = theta1;
            cosTheta = nextCosTheta;
            sinTheta = nextSinTheta;
        }
    }

    /**
     *
     * cube
     *
     * @param poseStack The stack used to store the transformation matrix.
     * @param buffer    Vertex consumer, which is used to cache vertex data.
     * @param x1        The x coordinate of the first corner of the cube.
     * @param y1        The y coordinate of the first corner of the cube.
     * @param z1        The z coordinate of the first corner of the cube.
     * @param x2        The x coordinate of the second corner of the cube.
     * @param y2        The y coordinate of the second corner of the cube.
     * @param z2        The z coordinate of the second corner of the cube.
     * @param red       color
     * @param green     color
     * @param blue      color
     * @param alpha     transparency
     */
    public static void shapeCube(PoseStack poseStack, VertexConsumer buffer, float x1, float y1, float z1,
                                 float x2, float y2, float z2, float red, float green, float blue, float alpha) {
        Matrix4f mat = poseStack.last().pose();

        // Determine the min and max coordinates for each axis
        float minX = Math.min(x1, x2);
        float maxX = Math.max(x1, x2);
        float minY = Math.min(y1, y2);
        float maxY = Math.max(y1, y2);
        float minZ = Math.min(z1, z2);
        float maxZ = Math.max(z1, z2);

        // Define the 8 vertices of the cube
        float[][] vertices = {
                {minX, minY, minZ},
                {maxX, minY, minZ},
                {maxX, maxY, minZ},
                {minX, maxY, minZ},
                {minX, minY, maxZ},
                {maxX, minY, maxZ},
                {maxX, maxY, maxZ},
                {minX, maxY, maxZ}
        };

        // Define the 6 faces of the cube, each with 2 triangles (6 vertices)
        int[][] faces = {
                {0, 1, 2, 2, 3, 0}, // Front face
                {1, 5, 6, 6, 2, 1}, // Right face
                {5, 4, 7, 7, 6, 5}, // Back face
                {4, 0, 3, 3, 7, 4}, // Left face
                {3, 2, 6, 6, 7, 3}, // Top face
                {4, 5, 1, 1, 0, 4}  // Bottom face
        };

        // Iterate through each face and add the vertices
        for (int[] face : faces) {
            for (int index : face) {
                float[] vertex = vertices[index];
                buffer.addVertex(mat, vertex[0], vertex[1], vertex[2]).setColor(red, green, blue, alpha);
            }
        }
    }

    /**
     *
     * sphere
     *
     * @param poseStack The stack used to store the transformation matrix.
     * @param buffer    Vertex consumer, which is used to cache vertex data.
     * @param x         The x coordinate of the center of the sphere.
     * @param y         The y coordinate of the center of the sphere.
     * @param z         The z coordinate of the center of the sphere.
     * @param radius    The radius of the sphere.
     * @param stacks    The number of subdivisions of the latitude.
     * @param slices    The number of subdivisions of the longitude.
     * @param red       color
     * @param green     color
     * @param blue      color
     * @param alpha     transparency
     */
    public static void shapeSphere(PoseStack poseStack, VertexConsumer buffer, float x, float y, float z, float radius,
                                   int stacks, int slices, float red, float green, float blue, float alpha) {
        Matrix4f mat = poseStack.last().pose();
        float stackStep = (float) Math.PI / stacks; // The step size between each stack (latitude)
        float sliceStep = (float) (2.0 * Math.PI / slices); // The step size between each slice (longitude)

        // Iterate through each stack
        for (int i = 0; i < stacks; i++) {
            float stackAngle1 = i * stackStep;
            float stackAngle2 = (i + 1) * stackStep;

            // Calculate the sin and cos for the stack angles
            float sinStack1 = (float) Math.sin(stackAngle1);
            float cosStack1 = (float) Math.cos(stackAngle1);
            float sinStack2 = (float) Math.sin(stackAngle2);
            float cosStack2 = (float) Math.cos(stackAngle2);

            // Iterate through each slice
            for (int j = 0; j < slices; j++) {
                float sliceAngle1 = j * sliceStep;
                float sliceAngle2 = (j + 1) * sliceStep;

                // Calculate the sin and cos for the slice angles
                float sinSlice1 = (float) Math.sin(sliceAngle1);
                float cosSlice1 = (float) Math.cos(sliceAngle1);
                float sinSlice2 = (float) Math.sin(sliceAngle2);
                float cosSlice2 = (float) Math.cos(sliceAngle2);

                // Define the 4 vertices of the current quad
                float[] v1 = {x + radius * sinStack1 * cosSlice1, y + radius * cosStack1, z + radius * sinStack1 * sinSlice1};
                float[] v2 = {x + radius * sinStack2 * cosSlice1, y + radius * cosStack2, z + radius * sinStack2 * sinSlice1};
                float[] v3 = {x + radius * sinStack2 * cosSlice2, y + radius * cosStack2, z + radius * sinStack2 * sinSlice2};
                float[] v4 = {x + radius * sinStack1 * cosSlice2, y + radius * cosStack1, z + radius * sinStack1 * sinSlice2};

                // First triangle
                buffer.addVertex(mat, v1[0], v1[1], v1[2]).setColor(red, green, blue, alpha);
                buffer.addVertex(mat, v2[0], v2[1], v2[2]).setColor(red, green, blue, alpha);
                buffer.addVertex(mat, v3[0], v3[1], v3[2]).setColor(red, green, blue, alpha);

                // Second triangle
                buffer.addVertex(mat, v3[0], v3[1], v3[2]).setColor(red, green, blue, alpha);
                buffer.addVertex(mat, v4[0], v4[1], v4[2]).setColor(red, green, blue, alpha);
                buffer.addVertex(mat, v1[0], v1[1], v1[2]).setColor(red, green, blue, alpha);
            }
        }
    }

    /**
     * Solid cylinder (open-ended tube), used as a gizmo axis shaft. The base circle is centred at
     * {@code (x, y, z)} and the cylinder extends {@code height} along {@code axis}. Emitted as TRIANGLES.
     */
    public static void shapeCylinder(PoseStack poseStack, VertexConsumer buffer, float x, float y, float z,
                                     float radius, float height, int segments,
                                     float red, float green, float blue, float alpha, Direction.Axis axis) {
        Matrix4f mat = poseStack.last().pose();
        float segmentDelta = (float) (2.0 * Math.PI / segments);
        for (int i = 0; i < segments; i++) {
            float t0 = i * segmentDelta;
            float t1 = (i + 1) * segmentDelta;
            float c0 = Mth.cos(t0), s0 = Mth.sin(t0);
            float c1 = Mth.cos(t1), s1 = Mth.sin(t1);

            float b0x, b0y, b0z, b1x, b1y, b1z, t0x, t0y, t0z, t1x, t1y, t1z;
            switch (axis) {
                case X -> {
                    b0x = x;          b0y = y + c0 * radius; b0z = z + s0 * radius;
                    b1x = x;          b1y = y + c1 * radius; b1z = z + s1 * radius;
                    t0x = x + height; t0y = b0y;             t0z = b0z;
                    t1x = x + height; t1y = b1y;             t1z = b1z;
                }
                case Y -> {
                    b0x = x + c0 * radius; b0y = y;          b0z = z + s0 * radius;
                    b1x = x + c1 * radius; b1y = y;          b1z = z + s1 * radius;
                    t0x = b0x;             t0y = y + height; t0z = b0z;
                    t1x = b1x;             t1y = y + height; t1z = b1z;
                }
                default -> { // Z
                    b0x = x + c0 * radius; b0y = y + s0 * radius; b0z = z;
                    b1x = x + c1 * radius; b1y = y + s1 * radius; b1z = z;
                    t0x = b0x;             t0y = b0y;             t0z = z + height;
                    t1x = b1x;             t1y = b1y;             t1z = z + height;
                }
            }
            // side quad as two triangles
            buffer.addVertex(mat, b0x, b0y, b0z).setColor(red, green, blue, alpha);
            buffer.addVertex(mat, b1x, b1y, b1z).setColor(red, green, blue, alpha);
            buffer.addVertex(mat, t1x, t1y, t1z).setColor(red, green, blue, alpha);
            buffer.addVertex(mat, t1x, t1y, t1z).setColor(red, green, blue, alpha);
            buffer.addVertex(mat, t0x, t0y, t0z).setColor(red, green, blue, alpha);
            buffer.addVertex(mat, b0x, b0y, b0z).setColor(red, green, blue, alpha);
        }
    }

    /**
     * Filled circular sector (pie slice), triangle-fan from {@code center}, laid out in the plane spanned by the
     * orthonormal vectors {@code u} and {@code v}. Sweeps {@code sweepAngle} radians from {@code startAngle}.
     * Emitted double-sided so it is visible from either side of the plane (e.g. the rotation angle indicator).
     */
    public static void shapeSector(PoseStack poseStack, VertexConsumer buffer, Vector3f center, Vector3f u, Vector3f v,
                                   float radius, float startAngle, float sweepAngle, int segments,
                                   float red, float green, float blue, float alpha) {
        Matrix4f mat = poseStack.last().pose();
        if (segments < 1) segments = 1;
        float step = sweepAngle / segments;
        for (int i = 0; i < segments; i++) {
            float a0 = startAngle + step * i;
            float a1 = startAngle + step * (i + 1);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float p0x = center.x + (u.x * c0 + v.x * s0) * radius;
            float p0y = center.y + (u.y * c0 + v.y * s0) * radius;
            float p0z = center.z + (u.z * c0 + v.z * s0) * radius;
            float p1x = center.x + (u.x * c1 + v.x * s1) * radius;
            float p1y = center.y + (u.y * c1 + v.y * s1) * radius;
            float p1z = center.z + (u.z * c1 + v.z * s1) * radius;
            // front face
            buffer.addVertex(mat, center.x, center.y, center.z).setColor(red, green, blue, alpha);
            buffer.addVertex(mat, p0x, p0y, p0z).setColor(red, green, blue, alpha);
            buffer.addVertex(mat, p1x, p1y, p1z).setColor(red, green, blue, alpha);
            // back face
            buffer.addVertex(mat, center.x, center.y, center.z).setColor(red, green, blue, alpha);
            buffer.addVertex(mat, p1x, p1y, p1z).setColor(red, green, blue, alpha);
            buffer.addVertex(mat, p0x, p0y, p0z).setColor(red, green, blue, alpha);
        }
    }

}
