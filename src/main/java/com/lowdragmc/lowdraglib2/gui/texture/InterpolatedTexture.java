package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;

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

    @LDLRegisterClient(name = "interpolated", registry = "ldlib2:gui_texture_renderer")
    public static final class InterpolatedTextureRenderer implements RegisteredGuiTextureRenderer<InterpolatedTexture, InterpolatedTextureRenderer> {
        @Override
        public Class<InterpolatedTexture> type() {
            return InterpolatedTexture.class;
        }

        @Override
        public void draw(InterpolatedTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            context.drawTexture(texture.from().copy(), x, y, width, height);
            context.drawTexture(texture.to().copy().setColor(ColorUtils.color(texture.lerp(), texture.lerp(), texture.lerp(), texture.lerp())),
                    x, y, width, height);
        }
    }
}
