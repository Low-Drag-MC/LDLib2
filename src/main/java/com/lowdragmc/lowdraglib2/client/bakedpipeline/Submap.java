package com.lowdragmc.lowdraglib2.client.bakedpipeline;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * @author KilaBash
 * @date 2023/3/23
 * @implNote Submap
 */
@Getter
@AllArgsConstructor
public class Submap implements ISubmap {
    // Submap split a cube face to four sub faces.
    public static final Submap[] uvs = new Submap[]{
            new Submap(4.0F, 4.0F, 0.0F, 0.0F),
            new Submap(4.0F, 4.0F, 4.0F, 0.0F),
            new Submap(4.0F, 4.0F, 8.0F, 0.0F),
            new Submap(4.0F, 4.0F, 12.0F, 0.0F),
            new Submap(4.0F, 4.0F, 0.0F, 4.0F),
            new Submap(4.0F, 4.0F, 4.0F, 4.0F),
            new Submap(4.0F, 4.0F, 8.0F, 4.0F),
            new Submap(4.0F, 4.0F, 12.0F, 4.0F),
            new Submap(4.0F, 4.0F, 0.0F, 8.0F),
            new Submap(4.0F, 4.0F, 4.0F, 8.0F),
            new Submap(4.0F, 4.0F, 8.0F, 8.0F),
            new Submap(4.0F, 4.0F, 12.0F, 8.0F),
            new Submap(4.0F, 4.0F, 0.0F, 12.0F),
            new Submap(4.0F, 4.0F, 4.0F, 12.0F),
            new Submap(4.0F, 4.0F, 8.0F, 12.0F),
            new Submap(4.0F, 4.0F, 12.0F, 12.0F),
            new Submap(8.0F, 8.0F, 0.0F, 0.0F),
            new Submap(8.0F, 8.0F, 8.0F, 0.0F),
            new Submap(8.0F, 8.0F, 0.0F, 8.0F),
            new Submap(8.0F, 8.0F, 8.0F, 8.0F)};
    public static final Submap FULL_TEXTURE = new Submap(16.0F, 16.0F, 0.0F, 0.0F);
    
    public static final Submap X1 = new Submap(16, 16, 0, 0);

    public static final Submap[][] X2 = new Submap[][] {
            { new Submap(8, 8, 0, 0), new Submap(8, 8, 8, 0) },
            { new Submap(8, 8, 0, 8), new Submap(8, 8, 8, 8) }
    };

    private static final float DIV3 = 16 / 3f;
    public static final Submap[][] X3 = new Submap[][] {
            { new Submap(DIV3, DIV3, 0, 0),         new Submap(DIV3, DIV3, DIV3, 0),        new Submap(DIV3, DIV3, DIV3 * 2, 0) },
            { new Submap(DIV3, DIV3, 0, DIV3),      new Submap(DIV3, DIV3, DIV3, DIV3),     new Submap(DIV3, DIV3, DIV3 * 2, DIV3) },
            { new Submap(DIV3, DIV3, 0, DIV3 * 2),  new Submap(DIV3, DIV3, DIV3, DIV3 * 2), new Submap(DIV3, DIV3, DIV3 * 2, DIV3 * 2) },
    };

    public static final Submap[][] X4 = new Submap[][] {
            { new Submap(4, 4, 0, 0),   new Submap(4, 4, 4, 0),     new Submap(4, 4, 8, 0),     new Submap(4, 4, 12, 0) },
            { new Submap(4, 4, 0, 4),   new Submap(4, 4, 4, 4),     new Submap(4, 4, 8, 4),     new Submap(4, 4, 12, 4) },
            { new Submap(4, 4, 0, 8),   new Submap(4, 4, 4, 8),     new Submap(4, 4, 8, 8),     new Submap(4, 4, 12, 8) },
            { new Submap(4, 4, 0, 12),  new Submap(4, 4, 4, 12),    new Submap(4, 4, 8, 12),    new Submap(4, 4, 12, 12) },
    };

    public final float width, height;
    public final float xOffset, yOffset;

    private final SubmapNormalized normalized = new SubmapNormalized(this);

    public float getInterpolatedU(TextureAtlasSprite sprite, float u) {
        return sprite.getU(getXOffset() + u / getWidth());
    }

    public float getInterpolatedV(TextureAtlasSprite sprite, float v) {
        return sprite.getV(getYOffset() + v / getWidth());
    }

    public float[] toArray() {
        return new float[] { getXOffset(), getYOffset(), getXOffset() + getWidth(), getYOffset() + getHeight() };
    }

    public SubmapNormalized normalize() {
        return normalized;
    }

    public Submap relativize() {
        return this;
    }

}
