package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The game window itself — the surface every UI used to be drawn into implicitly.
 *
 * <p>Nothing is cached: the main render target is swapped out on resize and the gui scale changes
 * with the option, so every accessor reads through.
 */
@OnlyIn(Dist.CLIENT)
public enum MainWindowSurface implements UISurface {
    INSTANCE;

    @Override
    public RenderTarget target() {
        return Minecraft.getInstance().getMainRenderTarget();
    }

    @Override
    public double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    @Override
    public int screenWidth() {
        return Minecraft.getInstance().getWindow().getScreenWidth();
    }

    @Override
    public int screenHeight() {
        return Minecraft.getInstance().getWindow().getScreenHeight();
    }

    @Override
    public long windowHandle() {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    @Override
    public boolean isMainWindow() {
        return true;
    }

    @Override
    public int guiScaledWidth() {
        // Minecraft already computes these and rounds its own way; matching it exactly matters more
        // than recomputing from the framebuffer.
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    @Override
    public int guiScaledHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }
}
