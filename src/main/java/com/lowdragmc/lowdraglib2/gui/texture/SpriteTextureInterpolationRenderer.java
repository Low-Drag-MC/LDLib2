package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SpriteTextureInterpolationRenderer {
    private SpriteTextureInterpolationRenderer() {
    }

    public static void draw(SpriteTextureInterpolation texture, GUIContext context, float x, float y, float width, float height) {
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
