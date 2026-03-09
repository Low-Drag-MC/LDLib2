package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ItemStackTextureRenderer {
    private ItemStackTextureRenderer() {
    }

    public static void draw(ItemStackTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, ItemStackTextureRenderer::drawInternal);
    }

    private static void drawInternal(ItemStackTexture texture, GUIContext context, float x, float y, float width, float height) {
        if (texture.items.length == 0) {
            return;
        }
        ItemStackTextureClientSupport.updateTick(texture);
        if (texture.items[texture.index].isEmpty()) {
            return;
        }
        context.pose.pushPose();
        context.pose.scale(width / 16f, height / 16f);
        context.pose.translate(x * 16 / width, y * 16 / height);
        DrawerHelperClient.drawItemStack(context, texture.items[texture.index], 0, 0, 0);
        context.pose.popPose();
    }
}
