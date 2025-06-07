package com.lowdragmc.lowdraglib2.client.bakedpipeline;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote FaceQuadBuilder
 */
@OnlyIn(Dist.CLIENT)

public class FaceQuad {
    public static final AABB BLOCK = new AABB(0, 0, 0, 1, 1, 1);

    /**
     * bake a quad of specific face.
     * @param cube cube model
     * @param face face of the quad
     * @param sprite texture
     * @param rotation additional rotation
     * @param tintIndex tint color index
     * @param emissivity emissivity
     * @param cull whether cull the face
     * @param shade whether shade the face
     */
    public static BakedQuad bakeFace(AABB cube, Direction face, TextureAtlasSprite sprite, ModelState rotation, int tintIndex, int emissivity, boolean cull, boolean shade) {
        return FaceQuadBakery.bakeQuad(
                new Vector3f((float) cube.minX * 16f, (float) cube.minY * 16f, (float) cube.minZ * 16f),
                new Vector3f((float) cube.maxX * 16f, (float) cube.maxY * 16f, (float) cube.maxZ * 16f),
                new BlockElementFace(cull ? face : null, tintIndex, "", new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0)),
                sprite,
                face,
                rotation,
                null,
                shade,
                emissivity
        );
    }

    public static BakedQuad bakeFace(Direction face, TextureAtlasSprite sprite, ModelState rotation, int tintIndex, int emissivity, boolean cull, boolean shade) {
        return bakeFace(BLOCK, face, sprite, rotation, tintIndex, emissivity, cull, shade);
    }

    public static BakedQuad bakeFace(Direction face, TextureAtlasSprite sprite, ModelState rotation, int tintIndex, int emissivity) {
        return bakeFace(face, sprite, rotation, tintIndex, emissivity, true, true);
    }

    public static BakedQuad bakeFace(Direction face, TextureAtlasSprite sprite, ModelState rotation, int tintIndex) {
        return bakeFace(face, sprite, rotation, tintIndex, 0);
    }

    public static BakedQuad bakeFace(Direction face, TextureAtlasSprite sprite, ModelState rotation) {
        return bakeFace(face, sprite, rotation, -1);
    }

    public static BakedQuad bakeFace(Direction face, TextureAtlasSprite sprite) {
        return bakeFace(face, sprite, BlockModelRotation.X0_Y0);
    }

    public static Builder builder(Direction face, TextureAtlasSprite sprite) {
        return new Builder(face, sprite);
    }

    @Accessors(chain = true, fluent = true)
    public static class Builder {
        @Setter
        Vector3f from = new Vector3f(0, 0, 0);
        @Setter
        Vector3f to = new Vector3f(16, 16, 16);
        @Setter
        Direction face;
        @Setter
        TextureAtlasSprite sprite;
        @Setter
        ModelState rotation = BlockModelRotation.X0_Y0;
        @Setter
        int tintIndex = -1, emissivity = 0;
        @Setter
        boolean cull = true, shade = true;
        @Setter
        UVPair uv0 = new UVPair(0, 0), uv1 = new UVPair(16, 16);

        protected Builder(Direction face, TextureAtlasSprite sprite) {
            this.face = face;
            this.sprite = sprite;
        }

        public Builder cube(AABB cube) {
            from = new Vector3f((float) cube.minX * 16f, (float) cube.minY * 16f, (float) cube.minZ * 16f);
            to = new Vector3f((float) cube.maxX * 16f, (float) cube.maxY * 16f, (float) cube.maxZ * 16f);
            return this;
        }

        public Builder uv0(float u0, float v0) {
            uv0 = new UVPair(u0, v0);
            return this;
        }

        public Builder uv1(float u1, float v1) {
            uv1 = new UVPair(u1, v1);
            return this;
        }

        /**
         * Making texture uv cut by the cube size.
         */
        public Builder cubeUV() {
            return switch (face) {
                case UP -> uv0(from.x(), from.z()).uv1(to.x(), to.z());
                case DOWN -> uv0(from.x(), to.z()).uv1(to.x(), from.z());
                case NORTH -> uv0(to.x(), to.y()).uv1(from.x(), from.y());
                case SOUTH -> uv0(from.x(), to.y()).uv1(to.x(), from.y());
                case WEST -> uv0(from.z(), to.y()).uv1(to.z(), from.y());
                case EAST -> uv0(to.z(), to.y()).uv1(from.z(), from.y());
            };
        }

        public BakedQuad bake() {
            return FaceQuadBakery.bakeQuad(
                    from,
                    to,
                    new BlockElementFace(cull ? face : null, tintIndex, "", new BlockFaceUV(new float[]{uv0.u(), uv0.v(), uv1.u(), uv1.v()}, 0)),
                    sprite,
                    face,
                    rotation,
                    null,
                    shade,
                    emissivity
            );
        }
    }

}

