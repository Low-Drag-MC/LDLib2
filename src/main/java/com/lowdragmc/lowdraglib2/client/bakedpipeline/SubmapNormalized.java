package com.lowdragmc.lowdraglib2.client.bakedpipeline;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * @author KilaBash
 * @date 2023/3/23
 * @implNote SubmapNormalized
 */
public class SubmapNormalized implements ISubmap {
    private static final float FACTOR = 16f;

    private final Submap parent;

    public SubmapNormalized(Submap submap) {
        this.parent = submap;
    }

    @Override
    public float getXOffset() {
        return parent.getXOffset() / FACTOR;
    }

    @Override
    public float getYOffset() {
        return parent.getYOffset() / FACTOR;
    }

    @Override
    public float getWidth() {
        return parent.getWidth() / FACTOR;
    }

    @Override
    public float getHeight() {
        return parent.getHeight() / FACTOR;
    }

    @Override
    public Submap relativize() {
        return parent;
    }

    @Override
    public SubmapNormalized normalize() {
        return this;
    }

    @Override
    public float getInterpolatedU(TextureAtlasSprite sprite, float u) {
        return parent.getInterpolatedU(sprite, u);
    }

    @Override
    public float getInterpolatedV(TextureAtlasSprite sprite, float v) {
        return parent.getInterpolatedV(sprite, v);
    }

    @Override
    public float[] toArray() {
        return parent.toArray();
    }
}
