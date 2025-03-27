package com.lowdragmc.lowdraglib.client.scene;

import com.lowdragmc.lowdraglib.math.PositionedRect;
import com.mojang.blaze3d.platform.Window;
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

    @Override
    public PositionedRect getPositionedRect(int x, int y, int width, int height) {
        Window window = Minecraft.getInstance().getWindow();
        //compute window size from scaled width & height
        int windowWidth = (int) (width / (window.getGuiScaledWidth() * 1.0) * window.getWidth());
        int windowHeight = (int) (height / (window.getGuiScaledHeight() * 1.0) * window.getHeight());
        //translate gui coordinates to window's ones (y is inverted)
        int windowX = (int) (x / (window.getGuiScaledWidth() * 1.0) * window.getWidth());
        int windowY = window.getHeight() - (int) (y / (window.getGuiScaledHeight() * 1.0) * window.getHeight()) - windowHeight;

        return super.getPositionedRect(windowX, windowY, windowWidth, windowHeight);
    }

    @Override
    public PositionedRect getPositionRectRevert(int windowX, int windowY, int windowWidth, int windowHeight) {
        Window window = Minecraft.getInstance().getWindow();
        //compute window size from scaled width & height
        int width = windowWidth * window.getGuiScaledWidth() / window.getWidth();
        int height = windowHeight * window.getGuiScaledHeight() / window.getHeight();
        //translate window coordinates to gui's ones (y is inverted)
        int x = windowX  * window.getGuiScaledWidth() / window.getWidth();
        int y = (window.getHeight() - windowY - windowHeight) * window.getGuiScaledHeight() / window.getHeight();

        return super.getPositionRectRevert(x, y, width, height);
    }

    @Override
    protected void clearView(int x, int y, int width, int height) {
        int a = (clearColor & 0xFF000000) >> 24;
        if (a == 0) {
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            return;
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, width, height);
        super.clearView(x, y, width, height);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
