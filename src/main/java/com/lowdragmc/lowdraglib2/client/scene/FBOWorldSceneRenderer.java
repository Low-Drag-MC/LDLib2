package com.lowdragmc.lowdraglib2.client.scene;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import com.lowdragmc.lowdraglib2.client.RenderTargetScope;

/**
 * @Author: KilaBash
 * @Description: FBO-based scene renderer using GPU textures.
 * Renders scene to an offscreen texture that can be blitted into the GUI.
 */
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
        BlockHitResult looking;
        try (var ignored = redirectToFBO()) {
            looking = super.screenPos2BlockPosFace(mouseX, mouseY, 0, 0, this.resolutionWidth, this.resolutionHeight);
        }
        return looking;
    }

    public Vector3f blockPos2ScreenPos(BlockPos pos, boolean depth) {
        Vector3f winPos;
        try (var ignored = redirectToFBO()) {
            winPos = super.blockPos2ScreenPos(pos, 0, 0, this.resolutionWidth, this.resolutionHeight);
        }
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
        try (var ignored = redirectToFBO()) {
            renderDirect(this.resolutionWidth, this.resolutionHeight,
                    (int) (this.resolutionWidth * (mouseX - x) / width),
                    (int) (this.resolutionHeight * (1 - (mouseY - y) / height)));
        }
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

    /**
     * Redirects drawing into this renderer's own textures until the returned scope is closed.
     *
     * <p>A scope rather than a matched {@code setup}/{@code teardown} pair because closing restores
     * <em>whatever was redirected before</em> instead of clearing to the game's own frame. That
     * matters as soon as a scene is not being drawn straight into the game window: nested inside a
     * visual-layer picture-in-picture pass, or inside a UI hosted in another OS window, clearing the
     * override would send everything drawn afterwards to the wrong place.
     */
    private RenderTargetScope redirectToFBO() {
        ensureFBOCreated();
        return RenderTargetScope.redirect(this.colorTextureView, this.depthTextureView);
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
        // 9 (RENDER_ATTACHMENT | COPY_DST) + 2 (COPY_SRC) = 11.
        // COPY_SRC is required so WorldSceneRenderer.readDepthPixelAsync can copyTextureToBuffer
        // for hover/pick depth read-back. Vanilla's PIP framework omits COPY_SRC, so PIP-mode
        // depth read silently falls back to the cached sample.
        depthTexture = device.createTexture(() -> "SceneFBO/Depth", 11, depthFormat, resolutionWidth, resolutionHeight, 1, 1);
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
