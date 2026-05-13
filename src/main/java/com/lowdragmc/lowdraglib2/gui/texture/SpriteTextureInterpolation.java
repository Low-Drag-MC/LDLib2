package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;

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

    @LDLRegisterClient(name = "sprite_interpolated_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredSpriteTextureInterpolationRenderer implements RegisteredGuiTextureRenderer<SpriteTextureInterpolation, RegisteredSpriteTextureInterpolationRenderer> {
        @Override
        public Class<SpriteTextureInterpolation> type() {
            return SpriteTextureInterpolation.class;
        }

        @Override
        public void draw(SpriteTextureInterpolation texture, GUIContext context, float x, float y, float width, float height) {
            context.drawTexture(texture.from().copy(), x, y, width, height);
            var overlay = texture.to().copy();
            var currentColor = overlay.color;
            overlay.color = ColorUtils.color(
                    ColorUtils.alpha(currentColor) * texture.lerp(),
                    ColorUtils.red(currentColor) * texture.lerp(),
                    ColorUtils.green(currentColor) * texture.lerp(),
                    ColorUtils.blue(currentColor) * texture.lerp());
            context.drawTexture(overlay, x, y, width, height);
        }
    }
}
