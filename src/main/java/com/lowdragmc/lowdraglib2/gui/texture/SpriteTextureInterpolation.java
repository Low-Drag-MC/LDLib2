package com.lowdragmc.lowdraglib2.gui.texture;

public final class SpriteTextureInterpolation implements IGuiTexture {
    private final SpriteTexture from;
    private final SpriteTexture to;
    private final float lerp;

    private SpriteTextureInterpolation(SpriteTexture from, SpriteTexture to, float lerp) {
        this.from = from;
        this.to = to;
        this.lerp = lerp;
    }

    public static SpriteTextureInterpolation of(SpriteTexture from, SpriteTexture to, float lerp) {
        return new SpriteTextureInterpolation(from, to, lerp);
    }

    public SpriteTexture from() {
        return from;
    }

    public SpriteTexture to() {
        return to;
    }

    public float lerp() {
        return lerp;
    }

    @Override
    public IGuiTexture copy() {
        return new SpriteTextureInterpolation(from.copy(), to.copy(), lerp);
    }
}
