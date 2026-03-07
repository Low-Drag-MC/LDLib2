package com.lowdragmc.lowdraglib2.client.scene;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.opengl.GlStateManager;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

/**
 * @Author: KilaBash
 * @Description: FBO-based scene renderer using GPU textures.
 * Renders scene to an offscreen texture that can be blitted into the GUI.
 */
@OnlyIn(Dist.CLIENT)
public class FBOWorldSceneRenderer extends WorldSceneRenderer {
    @Getter
    private int resolutionWidth = 1080;
    @Getter
    private int resolutionHeight = 1080;
    @Nullable
    private GpuTexture colorTexture;
    @Nullable
    @Getter
    private GpuTextureView colorTextureView;
    @Nullable
    private GpuTexture depthTexture;
    @Nullable
    private GpuTextureView depthTextureView;

    public FBOWorldSceneRenderer(Level world, int resolutionWidth, int resolutionHeight) {
        super(world);
        setFBOSize(resolutionWidth, resolutionHeight);
    }

    /***
     * This will modify the size of the FBO. You'd better know what you're doing before you call it.
     */
    public void setFBOSize(int resolutionWidth, int resolutionHeight) {
        this.resolutionWidth = resolutionWidth;
        this.resolutionHeight = resolutionHeight;
        if (colorTexture != null) {
            destroyFBO();
            createFBO();
        }
    }

    public BlockHitResult screenPos2BlockPosFace(int mouseX, int mouseY) {
        setupFBORendering();
        BlockHitResult looking = super.screenPos2BlockPosFace(mouseX, mouseY, 0, 0, this.resolutionWidth, this.resolutionHeight);
        teardownFBORendering();
        return looking;
    }

    public Vector3f blockPos2ScreenPos(BlockPos pos, boolean depth) {
        setupFBORendering();
        Vector3f winPos = super.blockPos2ScreenPos(pos, depth, 0, 0, this.resolutionWidth, this.resolutionHeight);
        teardownFBORendering();
        return winPos;
    }

    /**
     * Draw the scene to the FBO texture.
     */
    public void drawScene(float x, float y, float width, float height, float mouseX, float mouseY) {
        ensureFBOCreated();

        // Clear the FBO textures
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);

        // Redirect rendering to our FBO textures
        setupFBORendering();
        renderDirect(this.resolutionWidth, this.resolutionHeight,
                (int) (this.resolutionWidth * (mouseX - x) / width),
                (int) (this.resolutionHeight * (1 - (mouseY - y) / height)));
        teardownFBORendering();
    }

    /**
     * Render scene to FBO and return - caller is responsible for blitting the texture.
     * Use {@link #getColorTextureView()} to get the texture for blitting.
     */
    public void render(@Nonnull PoseStack poseStack, float x, float y, float width, float height, float mouseX, float mouseY) {
        drawScene(x, y, width, height, mouseX, mouseY);
    }

    public void render(@Nonnull PoseStack poseStack, float x, float y, float width, float height, int mouseX, int mouseY) {
        render(poseStack, x, y, width, height, (float) mouseX, (float) mouseY);
    }

    private void setupFBORendering() {
        ensureFBOCreated();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        RenderSystem.outputColorTextureOverride = this.colorTextureView;
        RenderSystem.outputDepthTextureOverride = this.depthTextureView;
    }

    private void teardownFBORendering() {
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        GlStateManager._viewport(0, 0, mainTarget.width, mainTarget.height);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    private void ensureFBOCreated() {
        if (colorTexture == null) {
            createFBO();
        }
    }

    private void createFBO() {
        destroyFBO();
        var device = RenderSystem.getDevice();
        colorTexture = device.createTexture(() -> "SceneFBO/Color", 15, TextureFormat.RGBA8, resolutionWidth, resolutionHeight, 1, 1);
        colorTextureView = device.createTextureView(colorTexture);
        TextureFormat depthFormat = Minecraft.getInstance().getMainRenderTarget().getDepthTexture().getFormat();
        depthTexture = device.createTexture(() -> "SceneFBO/Depth", 9, depthFormat, resolutionWidth, resolutionHeight, 1, 1);
        depthTextureView = device.createTextureView(depthTexture);
    }

    private void destroyFBO() {
        if (colorTextureView != null) {
            colorTextureView.close();
            colorTextureView = null;
        }
        if (colorTexture != null) {
            colorTexture.close();
            colorTexture = null;
        }
        if (depthTextureView != null) {
            depthTextureView.close();
            depthTextureView = null;
        }
        if (depthTexture != null) {
            depthTexture.close();
            depthTexture = null;
        }
    }

    @Override
    public void releaseResource() {
        super.releaseResource();
        destroyFBO();
    }
}
