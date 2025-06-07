package com.lowdragmc.lowdraglib2.client.bakedpipeline;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockMath;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.QuadTransformers;
import org.joml.*;

import java.lang.Math;

import static net.minecraft.client.renderer.block.model.FaceBakery.calculateFacing;

public class FaceQuadBakery {
    public static final int VERTEX_INT_SIZE = 8;
    private static final float RESCALE_22_5 = 1.0F / (float)Math.cos((float) (Math.PI / 8)) - 1.0F;
    private static final float RESCALE_45 = 1.0F / (float)Math.cos((float) (Math.PI / 4)) - 1.0F;
    public static final int VERTEX_COUNT = 4;
    private static final int COLOR_INDEX = 3;
    public static final int UV_INDEX = 4;

    public static BakedQuad bakeQuad(
        Vector3f posFrom,
        Vector3f posTo,
        BlockElementFace face,
        TextureAtlasSprite sprite,
        Direction facing,
        ModelState transform,
        @javax.annotation.Nullable BlockElementRotation partRotation,
        boolean shade,
        int emissivity
    ) {
        BlockFaceUV blockfaceuv = face.uv();
        if (transform.isUvLocked()) {
            blockfaceuv = recomputeUVs(face.uv(), facing, transform.getRotation());
        }

        float[] afloat = new float[blockfaceuv.uvs.length];
        System.arraycopy(blockfaceuv.uvs, 0, afloat, 0, afloat.length);
        float f = sprite.uvShrinkRatio();
        float f1 = (blockfaceuv.uvs[0] + blockfaceuv.uvs[0] + blockfaceuv.uvs[2] + blockfaceuv.uvs[2]) / 4.0F;
        float f2 = (blockfaceuv.uvs[1] + blockfaceuv.uvs[1] + blockfaceuv.uvs[3] + blockfaceuv.uvs[3]) / 4.0F;
        blockfaceuv.uvs[0] = Mth.lerp(f, blockfaceuv.uvs[0], f1);
        blockfaceuv.uvs[2] = Mth.lerp(f, blockfaceuv.uvs[2], f1);
        blockfaceuv.uvs[1] = Mth.lerp(f, blockfaceuv.uvs[1], f2);
        blockfaceuv.uvs[3] = Mth.lerp(f, blockfaceuv.uvs[3], f2);
        int[] aint = makeVertices(blockfaceuv, sprite, facing, setupShape(posFrom, posTo), transform.getRotation(), partRotation, shade);
        Direction direction = calculateFacing(aint);
        System.arraycopy(afloat, 0, blockfaceuv.uvs, 0, afloat.length);
        if (partRotation == null) {
            recalculateWinding(aint, direction);
        }

        ClientHooks.fillNormal(aint, direction);
        ExtraFaceData data = face.faceData();
        BakedQuad quad = new BakedQuad(aint, face.tintIndex(), direction, sprite, shade, data.ambientOcclusion());
        if (!ExtraFaceData.DEFAULT.equals(data)) {
            QuadTransformers.applyingLightmap(data.blockLight(), data.skyLight()).processInPlace(quad);
            QuadTransformers.applyingColor(data.color()).processInPlace(quad);
        }
        LDLibQuadTransformers.settingEmissivity(emissivity).processInPlace(quad);

        return quad;
    }

    public static BlockFaceUV recomputeUVs(BlockFaceUV uv, Direction facing, Transformation modelRotation) {
        Matrix4f matrix4f = BlockMath.getUVLockTransform(modelRotation, facing).getMatrix();
        float f = uv.getU(uv.getReverseIndex(0));
        float f1 = uv.getV(uv.getReverseIndex(0));
        Vector4f vector4f = matrix4f.transform(new Vector4f(f / 16.0F, f1 / 16.0F, 0.0F, 1.0F));
        float f2 = 16.0F * vector4f.x();
        float f3 = 16.0F * vector4f.y();
        float f4 = uv.getU(uv.getReverseIndex(2));
        float f5 = uv.getV(uv.getReverseIndex(2));
        Vector4f vector4f1 = matrix4f.transform(new Vector4f(f4 / 16.0F, f5 / 16.0F, 0.0F, 1.0F));
        float f6 = 16.0F * vector4f1.x();
        float f7 = 16.0F * vector4f1.y();
        float f8;
        float f9;
        if (Math.signum(f4 - f) == Math.signum(f6 - f2)) {
            f8 = f2;
            f9 = f6;
        } else {
            f8 = f6;
            f9 = f2;
        }

        float f10;
        float f11;
        if (Math.signum(f5 - f1) == Math.signum(f7 - f3)) {
            f10 = f3;
            f11 = f7;
        } else {
            f10 = f7;
            f11 = f3;
        }

        float f12 = (float)Math.toRadians(uv.rotation);
        Matrix3f matrix3f = new Matrix3f(matrix4f);
        Vector3f vector3f = matrix3f.transform(new Vector3f(Mth.cos(f12), Mth.sin(f12), 0.0F));
        int i = Math.floorMod(-((int)Math.round(Math.toDegrees(Math.atan2((double)vector3f.y(), (double)vector3f.x())) / 90.0)) * 90, 360);
        return new BlockFaceUV(new float[]{f8, f10, f9, f11}, i);
    }

    private static int[] makeVertices(
        BlockFaceUV uvs,
        TextureAtlasSprite sprite,
        Direction orientation,
        float[] posDiv16,
        Transformation rotation,
        @javax.annotation.Nullable BlockElementRotation partRotation,
        boolean shade
    ) {
        int[] aint = new int[32];

        for(int i = 0; i < 4; ++i) {
            bakeVertex(aint, i, orientation, uvs, posDiv16, sprite, rotation, partRotation, shade);
        }

        return aint;
    }

    private static void bakeVertex(
        int[] vertexData,
        int vertexIndex,
        Direction facing,
        BlockFaceUV blockFaceUV,
        float[] posDiv16,
        TextureAtlasSprite sprite,
        Transformation rotation,
        @javax.annotation.Nullable BlockElementRotation partRotation,
        boolean shade
    ) {
        FaceInfo.VertexInfo faceinfo$vertexinfo = FaceInfo.fromFacing(facing).getVertexInfo(vertexIndex);
        Vector3f vector3f = new Vector3f(posDiv16[faceinfo$vertexinfo.xFace], posDiv16[faceinfo$vertexinfo.yFace], posDiv16[faceinfo$vertexinfo.zFace]);
        applyElementRotation(vector3f, partRotation);
        applyModelRotation(vector3f, rotation);
        fillVertex(vertexData, vertexIndex, vector3f, sprite, blockFaceUV);
    }

    private static void fillVertex(int[] vertexData, int vertexIndex, Vector3f vector, TextureAtlasSprite sprite, BlockFaceUV blockFaceUV) {
        int i = vertexIndex * VERTEX_INT_SIZE;
        vertexData[i] = Float.floatToRawIntBits(vector.x());
        vertexData[i + 1] = Float.floatToRawIntBits(vector.y());
        vertexData[i + 2] = Float.floatToRawIntBits(vector.z());
        vertexData[i + COLOR_INDEX] = -1;
        vertexData[i + UV_INDEX] = Float.floatToRawIntBits(sprite.getU(blockFaceUV.getU(vertexIndex) / 16.0F));
        vertexData[i + UV_INDEX + 1] = Float.floatToRawIntBits(sprite.getV(blockFaceUV.getV(vertexIndex) / 16.0F));
    }

    private static float[] setupShape(Vector3f min, Vector3f max) {
        float[] afloat = new float[Direction.values().length];
        afloat[FaceInfo.Constants.MIN_X] = min.x() / 16.0F;
        afloat[FaceInfo.Constants.MIN_Y] = min.y() / 16.0F;
        afloat[FaceInfo.Constants.MIN_Z] = min.z() / 16.0F;
        afloat[FaceInfo.Constants.MAX_X] = max.x() / 16.0F;
        afloat[FaceInfo.Constants.MAX_Y] = max.y() / 16.0F;
        afloat[FaceInfo.Constants.MAX_Z] = max.z() / 16.0F;
        return afloat;
    }

    private static void applyElementRotation(Vector3f vec, @javax.annotation.Nullable BlockElementRotation partRotation) {
        if (partRotation != null) {
            Vector3f vector3f;
            Vector3f vector3f1;
            switch (partRotation.axis()) {
                case X -> {
                    vector3f = new Vector3f(1.0F, 0.0F, 0.0F);
                    vector3f1 = new Vector3f(0.0F, 1.0F, 1.0F);
                }
                case Y -> {
                    vector3f = new Vector3f(0.0F, 1.0F, 0.0F);
                    vector3f1 = new Vector3f(1.0F, 0.0F, 1.0F);
                }
                case Z -> {
                    vector3f = new Vector3f(0.0F, 0.0F, 1.0F);
                    vector3f1 = new Vector3f(1.0F, 1.0F, 0.0F);
                }
                default ->
                    throw new IllegalArgumentException("There are only 3 axes");
            }

            Quaternionf quaternionf = new Quaternionf().rotationAxis(partRotation.angle() * (float) (Math.PI / 180.0), vector3f);
            if (partRotation.rescale()) {
                if (Math.abs(partRotation.angle()) == 22.5F) {
                    vector3f1.mul(RESCALE_22_5);
                } else {
                    vector3f1.mul(RESCALE_45);
                }

                vector3f1.add(1.0F, 1.0F, 1.0F);
            } else {
                vector3f1.set(1.0F, 1.0F, 1.0F);
            }

            rotateVertexBy(vec, new Vector3f(partRotation.origin()), new Matrix4f().rotation(quaternionf), vector3f1);
        }
    }

    public static void applyModelRotation(Vector3f pos, Transformation transform) {
        if (transform != Transformation.identity()) {
            rotateVertexBy(pos, new Vector3f(0.5F, 0.5F, 0.5F), transform.getMatrix(), new Vector3f(1.0F, 1.0F, 1.0F));
        }
    }

    private static void rotateVertexBy(Vector3f pos, Vector3f origin, Matrix4f transform, Vector3f scale) {
        Vector4f vector4f = transform.transform(new Vector4f(pos.x() - origin.x(), pos.y() - origin.y(), pos.z() - origin.z(), 1.0F));
        vector4f.mul(new Vector4f(scale, 1.0F));
        pos.set(vector4f.x() + origin.x(), vector4f.y() + origin.y(), vector4f.z() + origin.z());
    }

    private static void recalculateWinding(int[] vertices, Direction direction) {
        int[] aint = new int[vertices.length];
        System.arraycopy(vertices, 0, aint, 0, vertices.length);
        float[] afloat = new float[Direction.values().length];
        afloat[FaceInfo.Constants.MIN_X] = 999.0F;
        afloat[FaceInfo.Constants.MIN_Y] = 999.0F;
        afloat[FaceInfo.Constants.MIN_Z] = 999.0F;
        afloat[FaceInfo.Constants.MAX_X] = -999.0F;
        afloat[FaceInfo.Constants.MAX_Y] = -999.0F;
        afloat[FaceInfo.Constants.MAX_Z] = -999.0F;

        for(int i = 0; i < 4; ++i) {
            int j = 8 * i;
            float f = Float.intBitsToFloat(aint[j]);
            float f1 = Float.intBitsToFloat(aint[j + 1]);
            float f2 = Float.intBitsToFloat(aint[j + 2]);
            if (f < afloat[FaceInfo.Constants.MIN_X]) {
                afloat[FaceInfo.Constants.MIN_X] = f;
            }

            if (f1 < afloat[FaceInfo.Constants.MIN_Y]) {
                afloat[FaceInfo.Constants.MIN_Y] = f1;
            }

            if (f2 < afloat[FaceInfo.Constants.MIN_Z]) {
                afloat[FaceInfo.Constants.MIN_Z] = f2;
            }

            if (f > afloat[FaceInfo.Constants.MAX_X]) {
                afloat[FaceInfo.Constants.MAX_X] = f;
            }

            if (f1 > afloat[FaceInfo.Constants.MAX_Y]) {
                afloat[FaceInfo.Constants.MAX_Y] = f1;
            }

            if (f2 > afloat[FaceInfo.Constants.MAX_Z]) {
                afloat[FaceInfo.Constants.MAX_Z] = f2;
            }
        }

        FaceInfo faceinfo = FaceInfo.fromFacing(direction);

        for(int i1 = 0; i1 < 4; ++i1) {
            int j1 = 8 * i1;
            FaceInfo.VertexInfo faceinfo$vertexinfo = faceinfo.getVertexInfo(i1);
            float f8 = afloat[faceinfo$vertexinfo.xFace];
            float f3 = afloat[faceinfo$vertexinfo.yFace];
            float f4 = afloat[faceinfo$vertexinfo.zFace];
            vertices[j1] = Float.floatToRawIntBits(f8);
            vertices[j1 + 1] = Float.floatToRawIntBits(f3);
            vertices[j1 + 2] = Float.floatToRawIntBits(f4);

            for(int k = 0; k < 4; ++k) {
                int l = 8 * k;
                float f5 = Float.intBitsToFloat(aint[l]);
                float f6 = Float.intBitsToFloat(aint[l + 1]);
                float f7 = Float.intBitsToFloat(aint[l + 2]);
                if (Mth.equal(f8, f5) && Mth.equal(f3, f6) && Mth.equal(f4, f7)) {
                    vertices[j1 + 4] = aint[l + 4];
                    vertices[j1 + 4 + 1] = aint[l + 4 + 1];
                }
            }
        }
    }
}
