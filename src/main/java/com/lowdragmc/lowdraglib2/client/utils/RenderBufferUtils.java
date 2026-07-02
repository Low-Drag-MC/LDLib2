package com.lowdragmc.lowdraglib2.client.utils;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec2;
import oshi.util.tuples.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.RandomAccess;

@OnlyIn(Dist.CLIENT)
public class RenderBufferUtils {

    public static void drawLine(PoseStack.Pose pose, VertexConsumer buffer,
                                float fromX, float fromY, float fromZ,
                                float toX, float toY, float toZ,
                                float sr, float sg, float sb, float sa, float er, float eg, float eb, float ea) {
        float nx = toX - fromX;
        float ny = toY - fromY;
        float nz = toZ - fromZ;
        float len2 = nx * nx + ny * ny + nz * nz;
        if (len2 >= 1.0e-12f) {
            float invLen = (float) (1.0 / Math.sqrt(len2));
            nx *= invLen;
            ny *= invLen;
            nz *= invLen;
        } else {
            nx = 0;
            ny = 0;
            nz = 0;
        }
        buffer.addVertex(pose, fromX, fromY, fromZ).setColor(sr, sg, sb, sa)
                .setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, toX, toY, toZ).setColor(er, eg, eb, ea)
                .setNormal(pose, nx, ny, nz);
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endLastBatch();
        }
    }

    public static void drawLine(PoseStack.Pose pose, VertexConsumer buffer, Vector3f from, Vector3f to,
                                float sr, float sg, float sb, float sa, float er, float eg, float eb, float ea) {
        drawLine(pose, buffer, from.x, from.y, from.z, to.x, to.y, to.z, sr, sg, sb, sa, er, eg, eb, ea);
    }

    public static void drawLine(Matrix4f pose, VertexConsumer buffer,
                                float fromX, float fromY, float fromZ,
                                float toX, float toY, float toZ,
                                float sr, float sg, float sb, float sa, float er, float eg, float eb, float ea) {
        float nx = toX - fromX;
        float ny = toY - fromY;
        float nz = toZ - fromZ;
        float len2 = nx * nx + ny * ny + nz * nz;
        if (len2 >= 1.0e-12f) {
            float invLen = (float) (1.0 / Math.sqrt(len2));
            nx *= invLen;
            ny *= invLen;
            nz *= invLen;
        } else {
            nx = 0;
            ny = 0;
            nz = 0;
        }

        float tx = pose.m00() * nx + pose.m10() * ny + pose.m20() * nz;
        float ty = pose.m01() * nx + pose.m11() * ny + pose.m21() * nz;
        float tz = pose.m02() * nx + pose.m12() * ny + pose.m22() * nz;
        float transformedLen2 = tx * tx + ty * ty + tz * tz;
        if (transformedLen2 >= 1.0e-12f) {
            float invTransformedLen = (float) (1.0 / Math.sqrt(transformedLen2));
            tx *= invTransformedLen;
            ty *= invTransformedLen;
            tz *= invTransformedLen;
        } else {
            tx = 0;
            ty = 0;
            tz = 0;
        }

        buffer.addVertex(pose, fromX, fromY, fromZ).setColor(sr, sg, sb, sa)
                .setNormal(tx, ty, tz);
        buffer.addVertex(pose, toX, toY, toZ).setColor(er, eg, eb, ea)
                .setNormal(tx, ty, tz);
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endLastBatch();
        }
    }

    public static void drawLine(Matrix4f pose, VertexConsumer buffer, Vector3f from, Vector3f to,
                                float sr, float sg, float sb, float sa, float er, float eg, float eb, float ea) {
        drawLine(pose, buffer, from.x, from.y, from.z, to.x, to.y, to.z, sr, sg, sb, sa, er, eg, eb, ea);
    }

    public static void drawLines(PoseStack poseStack, VertexConsumer buffer, List<Vector3f> points, int colorStart, int colorEnd) {
        int n = points.size();
        if (n < 2) return;

        float invPointCount = 1f / n;
        float colorMul = 1f / 255f;
        int sa = (colorStart >> 24) & 0xff, sr = (colorStart >> 16) & 0xff, sg = (colorStart >> 8) & 0xff, sb = colorStart & 0xff;
        int ea = (colorEnd >> 24) & 0xff, er = (colorEnd >> 16) & 0xff, eg = (colorEnd >> 8) & 0xff, eb = colorEnd & 0xff;
        ea = (ea - sa);
        er = (er - sr);
        eg = (eg - sg);
        eb = (eb - sb);
        Matrix4f mat = poseStack.last().pose();

        if (points instanceof RandomAccess) {
            Vector3f lastPoint = points.get(0);
            for (int i = 1; i < n; i++) {
                float s = (i - 1f) * invPointCount;
                float e = i * invPointCount;
                Vector3f point = points.get(i);
                drawLine(mat, buffer, lastPoint.x, lastPoint.y, lastPoint.z, point.x, point.y, point.z,
                        (sr + er * s) * colorMul, (sg + eg * s) * colorMul, (sb + eb * s) * colorMul, (sa + ea * s) * colorMul,
                        (sr + er * e) * colorMul, (sg + eg * e) * colorMul, (sb + eb * e) * colorMul, (sa + ea * e) * colorMul);
                lastPoint = point;
            }
        } else {
            var iter = points.iterator();
            Vector3f lastPoint = iter.next();
            for (int i = 1; i < n; i++) {
                float s = (i - 1f) * invPointCount;
                float e = i * invPointCount;
                Vector3f point = iter.next();
                drawLine(mat, buffer, lastPoint.x, lastPoint.y, lastPoint.z, point.x, point.y, point.z,
                        (sr + er * s) * colorMul, (sg + eg * s) * colorMul, (sb + eb * s) * colorMul, (sa + ea * s) * colorMul,
                        (sr + er * e) * colorMul, (sg + eg * e) * colorMul, (sb + eb * e) * colorMul, (sa + ea * e) * colorMul);
                lastPoint = point;
            }
        }
    }

    public static void drawCubeFrame(PoseStack poseStack, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        var mat = poseStack.last().pose();
        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a).setNormal(1,0,0);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a).setNormal(1,0,0);

        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a).setNormal(0,1,0);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a).setNormal(0,1,0);

        buffer.addVertex(mat, minX, minY, minZ).setColor(r, g, b, a).setNormal(0,0,1);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a).setNormal(0,0,1);

        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(1,0,0);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(1,0,0);

        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(0,1,0);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(0,1,0);

        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(0,0,1);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(r, g, b, a).setNormal(0,0,1);

        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a).setNormal(0,0,1);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(0,0,1);

        buffer.addVertex(mat, minX, maxY, minZ).setColor(r, g, b, a).setNormal(1,0,0);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(1,0,0);

        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a).setNormal(0,0,1);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(0,0,1);

        buffer.addVertex(mat, maxX, minY, minZ).setColor(r, g, b, a).setNormal(0,1,0);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(r, g, b, a).setNormal(0,1,0);

        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a).setNormal(1,0,0);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(r, g, b, a).setNormal(1,0,0);

        buffer.addVertex(mat, minX, minY, maxZ).setColor(r, g, b, a).setNormal(0,1,0);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(r, g, b, a).setNormal(0,1,0);
    }

    public static void drawCubeFace(PoseStack poseStack, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float a, boolean shade) {
        Matrix4f mat = poseStack.last().pose();
        float r = red, g = green, b = blue;

        if (minZ != maxZ && minY != maxY) {
            if (shade) {
                r *= 0.6f;
                g *= 0.6f;
                b *= 0.6f;
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
            float len2 = f * f + f1 * f1 + f2 * f2;
            if (len2 < 1.0e-12f) {
                continue;
            }
            float invLen = (float) (1.0 / Math.sqrt(len2));
            f *= invLen;
            f1 *= invLen;
            f2 *= invLen;

            buffer.addVertex(mat, a.x, a.y, a.z).setColor(color).setNormal(pose, f, f1, f2);
            buffer.addVertex(mat, b.x, b.y, b.z).setColor(color).setNormal(pose, f, f1, f2);
        }
    }

    public static void drawColorLines(@Nonnull PoseStack poseStack, VertexConsumer builder, List<Vector2f> points, int colorStart, int colorEnd, float width) {
        drawColorLines(poseStack, builder, points, colorStart, colorEnd, width, true);
    }

    public static void drawColorLines(@Nonnull PoseStack poseStack,
                                      VertexConsumer builder,
                                      List<Vector2f> points,
                                      int colorStart, int colorEnd,
                                      float halfWidth,
                                      boolean stripSide) {
        int n = points.size();
        if (n < 2) return;

        Matrix4f mat = poseStack.last().pose();

        int sa0 = (colorStart >>> 24) & 0xFF, sr0 = (colorStart >>> 16) & 0xFF, sg0 = (colorStart >>> 8) & 0xFF, sb0 = colorStart & 0xFF;
        int ea0 = (colorEnd   >>> 24) & 0xFF, er0 = (colorEnd   >>> 16) & 0xFF, eg0 = (colorEnd   >>> 8) & 0xFF, eb0 = colorEnd & 0xFF;

        int da = ea0 - sa0, dr = er0 - sr0, dg = eg0 - sg0, db = eb0 - sb0;
        float invSegCount = 1f / (n - 1);
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
                builder.addVertex(mat, aX, aY, 0).setColor(r0, g0, b0, a0);
            } else {
                // degenerate join: repeat the previous quad's last vertex and this quad's first vertex
                // (two zero-area triangles) so the two quads don't connect across the corner.
                builder.addVertex(mat, lastBx, lastBy, 0).setColor(r0, g0, b0, a0);
                builder.addVertex(mat, aX, aY, 0).setColor(r0, g0, b0, a0);
            }
            builder.addVertex(mat, aX, aY, 0).setColor(r0, g0, b0, a0);                       // prev + perp
            builder.addVertex(mat, prev.x - px, prev.y - py, 0).setColor(r0, g0, b0, a0);      // prev - perp
            builder.addVertex(mat, cur.x + px, cur.y + py, 0).setColor(r1, g1, b1, a1);        // cur + perp
            builder.addVertex(mat, eX, eY, 0).setColor(r1, g1, b1, a1);                        // cur - perp

            lastBx = eX; lastBy = eY;
            first = false;
            prev = cur; prevIdx = i; i++;
        }
    }

    public static void drawColorTexLines(@Nonnull PoseStack poseStack,
                                         VertexConsumer builder,
                                         List<Vector2f> points,
                                         int colorStart, int colorEnd,
                                         float halfWidth,
                                         boolean stripSide) {
        int n = points.size();
        if (n < 2) return;

        Matrix4f mat = poseStack.last().pose();

        int sa0 = (colorStart >>> 24) & 0xFF, sr0 = (colorStart >>> 16) & 0xFF, sg0 = (colorStart >>> 8) & 0xFF, sb0 = colorStart & 0xFF;
        int ea0 = (colorEnd   >>> 24) & 0xFF, er0 = (colorEnd   >>> 16) & 0xFF, eg0 = (colorEnd   >>> 8) & 0xFF, eb0 = colorEnd & 0xFF;

        int da = ea0 - sa0, dr = er0 - sr0, dg = eg0 - sg0, db = eb0 - sb0;

        float invSegCount = 1f / (n - 1);
        float colorMul = 1f / 255f;

        Vector2f last;
        Vector2f curr = null;
        float px = 0;
        float py = 0;
        boolean emittedAny = false;

        if (points instanceof RandomAccess) {
            last = points.get(0);
            for (int i = 1; i < n; i++) {
                float u = (i - 1f) * invSegCount;

                float r = (sr0 + dr * u) * colorMul;
                float g = (sg0 + dg * u) * colorMul;
                float b = (sb0 + db * u) * colorMul;
                float a = (sa0 + da * u) * colorMul;

                curr = points.get(i);

                float dx = curr.x - last.x;
                float dy = curr.y - last.y;
                float len2 = dx * dx + dy * dy;
                if (len2 < 1.0e-12f) {
                    last = curr;
                    continue;
                }

                float invLenHalfW = (float) (1.0 / Math.sqrt(len2)) * halfWidth;
                px = -dy * invLenHalfW;
                py = dx * invLenHalfW;

                builder.addVertex(mat, last.x + px, last.y + py, 0)
                        .setUv(u, 0)
                        .setColor(r, g, b, a);

                if (stripSide && !emittedAny) {
                    builder.addVertex(mat, last.x + px, last.y + py, 0)
                            .setUv(u, 0)
                            .setColor(r, g, b, a);
                }

                builder.addVertex(mat, last.x - px, last.y - py, 0)
                        .setUv(u, 1)
                        .setColor(r, g, b, a);

                emittedAny = true;
                last = curr;
            }
        } else {
            var iter = points.iterator();
            last = iter.next();
            for (int i = 1; i < n; i++) {
                float u = (i - 1f) * invSegCount;

                float r = (sr0 + dr * u) * colorMul;
                float g = (sg0 + dg * u) * colorMul;
                float b = (sb0 + db * u) * colorMul;
                float a = (sa0 + da * u) * colorMul;

                curr = iter.next();

                float dx = curr.x - last.x;
                float dy = curr.y - last.y;
                float len2 = dx * dx + dy * dy;
                if (len2 < 1.0e-12f) {
                    last = curr;
                    continue;
                }

                float invLenHalfW = (float) (1.0 / Math.sqrt(len2)) * halfWidth;
                px = -dy * invLenHalfW;
                py = dx * invLenHalfW;

                builder.addVertex(mat, last.x + px, last.y + py, 0)
                        .setUv(u, 0)
                        .setColor(r, g, b, a);

                if (stripSide && !emittedAny) {
                    builder.addVertex(mat, last.x + px, last.y + py, 0)
                            .setUv(u, 0)
                            .setColor(r, g, b, a);
                }

                builder.addVertex(mat, last.x - px, last.y - py, 0)
                        .setUv(u, 1)
                        .setColor(r, g, b, a);

                emittedAny = true;
                last = curr;
            }
        }

        if (!emittedAny || curr == null) return;

        float rEnd = (sr0 + dr) * colorMul;
        float gEnd = (sg0 + dg) * colorMul;
        float bEnd = (sb0 + db) * colorMul;
        float aEnd = (sa0 + da) * colorMul;

        builder.addVertex(mat, curr.x + px, curr.y + py, 0)
                .setUv(1, 0)
                .setColor(rEnd, gEnd, bEnd, aEnd);

        builder.addVertex(mat, curr.x - px, curr.y - py, 0)
                .setUv(1, 1)
                .setColor(rEnd, gEnd, bEnd, aEnd);

        if (stripSide) {
            builder.addVertex(mat, curr.x - px, curr.y - py, 0)
                    .setUv(1, 1)
                    .setColor(rEnd, gEnd, bEnd, aEnd);
        }
    }

    public static void drawCircleLine(@Nonnull PoseStack poseStack, VertexConsumer buffer,
                                      Vector3f position,
                                      Vector3f normal, int segments,
                                      float radius, float red, float green, float blue, float alpha) {

        Matrix4f pose = poseStack.last().pose();

        if (segments < 3) {
            segments = 3;
        }

        Vector3f u = new Vector3f();
        Vector3f v = new Vector3f();

        if (normal.x == 0 && normal.y == 0 && normal.z == 1) {
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

        float prevX = 0, prevY = 0, prevZ = 0;
        float firstX = 0, firstY = 0, firstZ = 0;
        double angleStep = 2.0 * Math.PI / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = angleStep * i;
            float x = (float) (radius * Math.cos(angle));
            float y = (float) (radius * Math.sin(angle));

            float currentX = position.x + u.x * x + v.x * y;
            float currentY = position.y + u.y * x + v.y * y;
            float currentZ = position.z + u.z * x + v.z * y;

            if (i > 0) {
                drawLine(pose, buffer, prevX, prevY, prevZ, currentX, currentY, currentZ, red, green, blue, alpha, red, green, blue, alpha);
            } else {
                firstX = currentX;
                firstY = currentY;
                firstZ = currentZ;
            }

            prevX = currentX;
            prevY = currentY;
            prevZ = currentZ;
        }

        drawLine(pose, buffer, prevX, prevY, prevZ, firstX, firstY, firstZ, red, green, blue, alpha, red, green, blue, alpha);
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

        buffer.addVertex(mat, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, minY, minZ).setColor(red, green, blue, alpha);

        buffer.addVertex(mat, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(red, green, blue, alpha);

        buffer.addVertex(mat, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(red, green, blue, alpha);

        buffer.addVertex(mat, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(red, green, blue, alpha);

        buffer.addVertex(mat, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, maxY, minZ).setColor(red, green, blue, alpha);

        buffer.addVertex(mat, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(mat, minX, minY, maxZ).setColor(red, green, blue, alpha);
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

                float v1x = x + radius * sinStack1 * cosSlice1;
                float v1y = y + radius * cosStack1;
                float v1z = z + radius * sinStack1 * sinSlice1;
                float v2x = x + radius * sinStack2 * cosSlice1;
                float v2y = y + radius * cosStack2;
                float v2z = z + radius * sinStack2 * sinSlice1;
                float v3x = x + radius * sinStack2 * cosSlice2;
                float v3y = y + radius * cosStack2;
                float v3z = z + radius * sinStack2 * sinSlice2;
                float v4x = x + radius * sinStack1 * cosSlice2;
                float v4y = y + radius * cosStack1;
                float v4z = z + radius * sinStack1 * sinSlice2;

                // First triangle
                buffer.addVertex(mat, v1x, v1y, v1z).setColor(red, green, blue, alpha);
                buffer.addVertex(mat, v2x, v2y, v2z).setColor(red, green, blue, alpha);
                buffer.addVertex(mat, v3x, v3y, v3z).setColor(red, green, blue, alpha);

                // Second triangle
                buffer.addVertex(mat, v3x, v3y, v3z).setColor(red, green, blue, alpha);
                buffer.addVertex(mat, v4x, v4y, v4z).setColor(red, green, blue, alpha);
                buffer.addVertex(mat, v1x, v1y, v1z).setColor(red, green, blue, alpha);
            }
        }
    }



}
