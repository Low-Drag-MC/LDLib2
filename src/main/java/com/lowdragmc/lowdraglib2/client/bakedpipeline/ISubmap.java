package com.lowdragmc.lowdraglib2.client.bakedpipeline;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * @author KilaBash
 * @date 2023/3/24
 * @implNote ISubmap
 */
public interface ISubmap {
    float getYOffset();

    float getXOffset();

    float getWidth();

    float getHeight();

    float getInterpolatedU(TextureAtlasSprite var1, float var2);

    float getInterpolatedV(TextureAtlasSprite var1, float var2);

    float[] toArray();

    ISubmap normalize();

    ISubmap relativize();
}
