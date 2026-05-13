package com.lowdragmc.lowdraglib2.gui.texture.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.TransformTexture;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TransformTextureRenderer {
    private TransformTextureRenderer() {
    }

    public static <T extends TransformTexture> void draw(
            T texture,
            GUIContext context,
            float x,
            float y,
            float width,
            float height,
            Drawer<T> drawer
    ) {
        preDraw(texture, context.graphics, x, y, width, height);
        drawer.draw(texture, context, x, y, width, height);
        postDraw(texture, context.graphics);
    }

    private static void preDraw(TransformTexture texture, GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
        texture.getTransform2D().pushPose(graphics.pose(), x, y, width, height);
    }

    private static void postDraw(TransformTexture texture, GuiGraphicsExtractor graphics) {
        texture.getTransform2D().popPose(graphics.pose());
    }

    @FunctionalInterface
    public interface Drawer<T extends TransformTexture> {
        void draw(T texture, GUIContext context, float x, float y, float width, float height);
    }
}
