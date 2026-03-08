package com.lowdragmc.lowdraglib2.gui.texture;

public final class InterpolatedTexture implements IGuiTexture {
    private final IGuiTexture from;
    private final IGuiTexture to;
    private final float lerp;

    private InterpolatedTexture(IGuiTexture from, IGuiTexture to, float lerp) {
        this.from = from;
        this.to = to;
        this.lerp = lerp;
    }

    public static InterpolatedTexture of(IGuiTexture from, IGuiTexture to, float lerp) {
        return new InterpolatedTexture(from, to, lerp);
    }

    public IGuiTexture from() {
        return from;
    }

    public IGuiTexture to() {
        return to;
    }

    public float lerp() {
        return lerp;
    }

    @Override
    public IGuiTexture copy() {
        return new InterpolatedTexture(from.copy(), to.copy(), lerp);
    }
}
