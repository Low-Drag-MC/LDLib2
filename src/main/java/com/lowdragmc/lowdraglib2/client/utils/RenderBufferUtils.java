package com.lowdragmc.lowdraglib2.client.utils;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec2;

import javax.annotation.Nonnull;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RenderBufferUtils {

    public static void drawLine(PoseStack.Pose pose, VertexConsumer buffer, Vector3f from, Vector3f to,
                                float sr, float sg, float sb, float sa, float er, float eg, float eb, float ea) {
        var normalDir = new Vector3f(to.x - from.x, to.y - from.y, to.z - from.z).normalize();
        buffer.addVertex(pose, from.x, from.y, from.z).setColor(sr, sg, sb, sa)
                .setNormal(pose, normalDir.x, normalDir.y, normalDir.z);
        buffer.addVertex(pose, to.x, to.y, to.z).setColor(er, eg, eb, ea)
                .setNormal(pose, normalDir.x, normalDir.y, normalDir.z);
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endLastBatch();
        }
    }

    public static void drawLine(Matrix4f pose, VertexConsumer buffer, Vector3f from, Vector3f to,
                                float sr, float sg, float sb, float sa, float er, float eg, float eb, float ea) {
        var normalDir = new Vector3f(to.x - from.x, to.y - from.y, to.z - from.z).normalize();
        normalDir = pose.transformDirection(normalDir);
        buffer.addVertex(pose, from.x, from.y, from.z).setColor(sr, sg, sb, sa)
                .setNormal(normalDir.x, normalDir.y, normalDir.z);
        buffer.addVertex(pose, to.x, to.y, to.z).setColor(er, eg, eb, ea)
                .setNormal(normalDir.x, normalDir.y, normalDir.z);
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endLastBatch();
        }
    }

    public static void drawLines(PoseStack poseStack, VertexConsumer buffer, List<Vector3f> points, int colorStart, int colorEnd) {
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
                    (sr + er * e) / 255, (sg + eg * e) / 255, (sb + eb * e) / 255, (sa + ea * e) / 255);
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

    public static void drawColorLines(@Nonnull PoseStack poseStack, VertexConsumer builder, List<Vec2> points, int colorStart, int colorEnd, float width) {
        if (points.size() < 2) return;
        Matrix4f mat = poseStack.last().pose();
        Vec2 lastPoint = points.get(0);
        Vec2 point = points.get(1);
        Vector3f vec = null;
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
            vec = new Vector3f(point.x - lastPoint.x, point.y - lastPoint.y, 0).rotateZ(Mth.HALF_PI).normalize().mul(-width);
            builder.addVertex(mat, lastPoint.x + vec.x, lastPoint.y + vec.y, 0)
                    .setColor((sr + er * s) / 255, (sg + eg * s) / 255, (sb + eb * s) / 255, (sa + ea * s) / 255)
                    ;
            vec.mul(-1);
            builder.addVertex(mat, lastPoint.x + vec.x, lastPoint.y + vec.y, 0)
                    .setColor((sr + er * e) / 255, (sg + eg * e) / 255, (sb + eb * e) / 255, (sa + ea * e) / 255)
                    ;
            lastPoint = point;
        }
        vec.mul(-1);
        builder.addVertex(mat, point.x + vec.x, point.y + vec.y, 0)
                .setColor(sr + er, sg + eg, sb + eb, sa + ea)
                ;
        vec.mul(-1);
        builder.addVertex(mat, point.x + vec.x, point.y + vec.y, 0)
                .setColor(sr + er, sg + eg, sb + eb, sa + ea)
                ;
    }

    public static void drawColorTexLines(@Nonnull PoseStack poseStack, VertexConsumer builder, List<Vec2> points, int colorStart, int colorEnd, float width) {
        if (points.size() < 2) return;
        Matrix4f mat = poseStack.last().pose();
        Vec2 lastPoint = points.get(0);
        Vec2 point = points.get(1);
        Vector3f vec = null;
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
            float u = (i - 1f) / points.size();
            vec = new Vector3f(point.x - lastPoint.x, point.y - lastPoint.y, 0).rotateZ(Mth.HALF_PI).normalize().mul(-width);
            builder.addVertex(mat, lastPoint.x + vec.x, lastPoint.y + vec.y, 0).setUv(u,0)
                    .setColor((sr + er * s) / 255, (sg + eg * s) / 255, (sb + eb * s) / 255, (sa + ea * s) / 255)
                    ;
            vec.mul(-1);
            builder.addVertex(mat, lastPoint.x + vec.x, lastPoint.y + vec.y, 0).setUv(u,1)
                    .setColor((sr + er * e) / 255, (sg + eg * e) / 255, (sb + eb * e) / 255, (sa + ea * e) / 255)
                    ;
            lastPoint = point;
        }
        vec.mul(-1);
        builder.addVertex(mat, point.x + vec.x, point.y + vec.y, 0).setUv(1,0)
                .setColor(sr + er, sg + eg, sb + eb, sa + ea)
                ;
        vec.mul(-1);
        builder.addVertex(mat, point.x + vec.x, point.y + vec.y, 0).setUv(1,1)
                .setColor(sr + er, sg + eg, sb + eb, sa + ea)
                ;
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
                drawLine(pose, buffer, prevPoint, currentPoint, red, green, blue, alpha, red, green, blue, alpha);
            } else {
                firstPoint.set(currentPoint);
            }

            prevPoint.set(currentPoint);
        }

        drawLine(pose, buffer, prevPoint, firstPoint, red, green, blue, alpha, red, green, blue, alpha);
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



}
