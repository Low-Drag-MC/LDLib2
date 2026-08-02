package com.lowdragmc.lowdraglib2.client.scene;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: KilaBash
 * @Date: 2021/08/23
 * @Description: It looks similar to {@link ImmediateWorldSceneRenderer}, but totally different.
 * It uses FBO and is more universality and efficient(X).
 * FBO can be rendered anywhere more flexibly, not just in the GUI.
 * If you have scene rendering needs, you will love this FBO renderer.
 */
@OnlyIn(Dist.CLIENT)
public class FBOWorldSceneRenderer extends WorldSceneRenderer {
    @Getter
    private int resolutionWidth = 1080;
    @Getter
    private int resolutionHeight = 1080;
    @Getter
    private RenderTarget fbo;
    /** The color the FBO clears to before each scene draw. An opaque background sidesteps every
     *  alpha-compositing concern when the FBO is drawn into a GUI; the transparent-black default
     *  preserves the historical overlay behavior. */
    private float clearRed = 0.0f, clearGreen = 0.0f, clearBlue = 0.0f, clearAlpha = 0.0f;

    public FBOWorldSceneRenderer(Level world, int resolutionWidth, int resolutionHeight) {
        super(world);
        setFBOSize(resolutionWidth, resolutionHeight);
    }

    public FBOWorldSceneRenderer(Level world, @Nonnull RenderTarget fbo) {
        super(world);
        this.fbo = fbo;
    }

    /** Set the color the FBO clears to before each scene draw (see {@link #clearRed}). */
    public FBOWorldSceneRenderer setClearColor(float red, float green, float blue, float alpha) {
        this.clearRed = red;
        this.clearGreen = green;
        this.clearBlue = blue;
        this.clearAlpha = alpha;
        return this;
    }

    /** The base render pass re-clears the (already bound) FBO each frame with a hardcoded transparent
     *  black — honour the configured clear color instead, else {@link #setClearColor} has no effect. */
    @Override
    protected void clearView(int x, int y, int width, int height) {
        RenderSystem.clearColor(clearRed, clearGreen, clearBlue, clearAlpha);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    /***
     * This will modify the size of the FBO. You'd better know what you're doing before you call it.
     * Invalid (non-positive) sizes are ignored — UI layouts transiently report 0x0 during relayout,
     * and resizing a render target to nothing crashes GL.
     */
    public void setFBOSize(int resolutionWidth, int resolutionHeight) {
        if (resolutionWidth <= 0 || resolutionHeight <= 0) {
            return;
        }
        this.resolutionWidth = resolutionWidth;
        this.resolutionHeight = resolutionHeight;
        if (fbo != null) {
            fbo.resize(resolutionWidth, resolutionHeight, Minecraft.ON_OSX);
        }
    }

    public BlockHitResult screenPos2BlockPosFace(int mouseX, int mouseY) {
        int lastID = bindFBO();
        BlockHitResult looking = super.screenPos2BlockPosFace(mouseX, mouseY, 0, 0, this.resolutionWidth, this.resolutionHeight);
        unbindFBO(lastID);
        return looking;
    }

    public Vector3f blockPos2ScreenPos(BlockPos pos, boolean depth){
        int lastID = bindFBO();
        Vector3f winPos = super.blockPos2ScreenPos(pos, depth, 0, 0, this.resolutionWidth, this.resolutionHeight);
        unbindFBO(lastID);
        return winPos;
    }

    public IGuiTexture drawAsTexture() {
        return (graphics, mouseX, mouseY, x, y, width, height, partialTicks) -> {
            if (!checkFBOValid()) {
                createFBO();
                drawScene(0, 0, this.resolutionWidth, this.resolutionHeight, 0, 0);
            }

            var poseStack = graphics.pose();
            var pose = poseStack.last().pose();

            var bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, fbo.getColorTextureId());

            bufferbuilder.addVertex(pose, x + width, y + height, 0).setUv(1, 0);
            bufferbuilder.addVertex(pose, x + width, y, 0).setUv(1, 1);
            bufferbuilder.addVertex(pose, x, y, 0).setUv(0, 1);
            bufferbuilder.addVertex(pose, x, y + height, 0).setUv(0, 0);

            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        };
    }

    public void drawScene(float x, float y, float width, float height, float mouseX, float mouseY) {
        // before the FBO is bound, otherwise the GUI geometry this drains would be drawn into it
        flushPendingGuiBatches();
        // bind to FBO
        int lastID = bindFBO();
        super.render(new PoseStack(), 0, 0, this.resolutionWidth, this.resolutionHeight, (int) (this.resolutionWidth * (mouseX - x) / width), (int) (this.resolutionHeight * (1 - (mouseY - y) / height)));
        // unbind FBO
        unbindFBO(lastID);
    }

    public void render(@Nonnull PoseStack poseStack, float x, float y, float width, float height, float mouseX, float mouseY) {
        drawScene(x, y, width, height, mouseX, mouseY);

        // render rect with FBO texture
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, fbo.getColorTextureId());

        var pose = poseStack.last().pose();
        bufferbuilder.addVertex(pose, x + width, y + height, 0).setUv(1, 0);
        bufferbuilder.addVertex(pose, x + width, y, 0).setUv(1, 1);
        bufferbuilder.addVertex(pose, x, y, 0).setUv(0, 1);
        bufferbuilder.addVertex(pose, x, y + height, 0).setUv(0, 0);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

    public void render(@Nonnull PoseStack poseStack, float x, float y, float width, float height, int mouseX, int mouseY) {
        render(poseStack, x, y, width, height, (float) mouseX, (float) mouseY);
    }

    private int bindFBO(){
        if (!checkFBOValid()) {
            createFBO();
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        int lastID = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
        fbo.setClearColor(clearRed, clearGreen, clearBlue, clearAlpha);
        fbo.clear(Minecraft.ON_OSX);
        fbo.bindWrite(true);
        return lastID;
    }

    private void unbindFBO(int lastID){
        fbo.unbindRead();
        GlStateManager._glBindFramebuffer(36160, lastID);
        var mainBuffer = Minecraft.getInstance().getMainRenderTarget();
        GlStateManager._viewport(0, 0, mainBuffer.viewWidth, mainBuffer.viewHeight);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    private boolean checkFBOValid() {
        if (fbo == null) return false;
        return fbo.frameBufferId >= 0;
    }

    private void createFBO() {
        int lastID = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
        releaseFBO();
        fbo = new MainTarget(resolutionWidth, resolutionHeight);
        GlStateManager._glBindFramebuffer(36160, lastID);
    }

    public void releaseFBO() {
        if (fbo != null) {
            if (RenderSystem.isOnRenderThread()) {
                fbo.destroyBuffers();
            } else {
                RenderSystem.recordRenderCall(() -> fbo.destroyBuffers());
            }
        }
    }

    @Override
    public void releaseResource() {
        super.releaseResource();
        releaseFBO();
    }
}
