package com.lowdragmc.lowdraglib2.integration.rei;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.rei.api.client.gui.Renderer;

/**
 * @author KilaBash
 * @date: 2022/04/30
 * @implNote IGui2Renderer
 */
public interface IGui2Renderer {
    static Renderer toDrawable(IGuiTexture guiTexture) {
        return (graphics, bounds, mouseX, mouseY, delta) -> {
            if (guiTexture == null) return;
            guiTexture.draw(graphics, mouseX, mouseY, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getWidth());
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        };
    }
}
