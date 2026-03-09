package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class FluidStackTextureRenderer {
    private FluidStackTextureRenderer() {
    }

    public static void draw(FluidStackTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, FluidStackTextureRenderer::drawInternal);
    }

    private static void drawInternal(FluidStackTexture texture, GUIContext context, float x, float y, float width, float height) {
        if (texture.fluids.length == 0) {
            return;
        }
        FluidStackTextureClientSupport.updateTick(texture);
        if (texture.fluids[texture.index].isEmpty()) {
            return;
        }
        DrawerHelperClient.drawFluidForGui(context, texture.fluids[texture.index], x, y, width, height, texture.color);
    }
}
