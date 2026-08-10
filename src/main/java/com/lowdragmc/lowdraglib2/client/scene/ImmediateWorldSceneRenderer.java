package com.lowdragmc.lowdraglib2.client.scene;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface;
import com.lowdragmc.lowdraglib2.math.PositionedRect;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.lwjgl.opengl.GL11;

/**
 * Created with IntelliJ IDEA.
 * @Author: KilaBash
 * @Date: 2021/8/24
 * @Description: Real-time rendering renderer.
 * If you need to render scene as a texture, use the FBO {@link FBOWorldSceneRenderer}.
 */
@OnlyIn(Dist.CLIENT)
public class ImmediateWorldSceneRenderer extends WorldSceneRenderer {

    public ImmediateWorldSceneRenderer(Level world) {
        super(world);
    }

    /**
     * Gui coordinates to framebuffer pixels, against the surface being drawn into.
     *
     * <p>Deliberately not {@code Minecraft.getWindow()}: both the scale and the vertical flip have to
     * be taken from the destination. Reading them from the game window puts the viewport somewhere
     * outside a smaller off-screen target, and the scene renders where nobody can see it — which is
     * what a scene in a floating window used to do.
     */
    @Override
    public PositionedRect getPositionedRect(int x, int y, int width, int height) {
        var surface = UISurface.current();
        int frameWidth = surface.framebufferWidth();
        int frameHeight = surface.framebufferHeight();
        int guiWidth = Math.max(1, surface.guiScaledWidth());
        int guiHeight = Math.max(1, surface.guiScaledHeight());
        //compute window size from scaled width & height
        int windowWidth = (int) (width / (guiWidth * 1.0) * frameWidth);
        int windowHeight = (int) (height / (guiHeight * 1.0) * frameHeight);
        //translate gui coordinates to window's ones (y is inverted)
        int windowX = (int) (x / (guiWidth * 1.0) * frameWidth);
        int windowY = frameHeight - (int) (y / (guiHeight * 1.0) * frameHeight) - windowHeight;

        return super.getPositionedRect(windowX, windowY, windowWidth, windowHeight);
    }

    @Override
    public PositionedRect getPositionRectRevert(int windowX, int windowY, int windowWidth, int windowHeight) {
        var surface = UISurface.current();
        int frameWidth = Math.max(1, surface.framebufferWidth());
        int frameHeight = surface.framebufferHeight();
        int guiWidth = surface.guiScaledWidth();
        int guiHeight = surface.guiScaledHeight();
        //compute window size from scaled width & height
        int width = windowWidth * guiWidth / frameWidth;
        int height = windowHeight * guiHeight / Math.max(1, frameHeight);
        //translate window coordinates to gui's ones (y is inverted)
        int x = windowX * guiWidth / frameWidth;
        int y = (frameHeight - windowY - windowHeight) * guiHeight / Math.max(1, frameHeight);

        return super.getPositionRectRevert(x, y, width, height);
    }

    @Override
    protected void clearView(int x, int y, int width, int height) {
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    @Override
    protected void resetCamera() {
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        super.resetCamera();
    }
}
