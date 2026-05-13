package com.lowdragmc.lowdraglib2.client.scene;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.NotNull;

/**
 * PictureInPicture renderer for WorldSceneRenderer.
 * Handles both FBO-based and immediate scene renderers:
 * - FBO: renders to its own textures, blits from those
 * - Immediate: renders directly into the PIP-managed textures
 */
public class ScenePIPRenderer extends PictureInPictureRenderer<SceneRenderState> {

    public ScenePIPRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<SceneRenderState> getRenderStateClass() {
        return SceneRenderState.class;
    }

    @Override
    protected void renderToTexture(SceneRenderState state, PoseStack poseStack) {
        var renderer = state.sceneRenderer();

        if (renderer instanceof FBOWorldSceneRenderer fboRenderer) {
            // FBO renderer manages its own textures.
            // Clear the PIP output overrides so the FBO can set its own.
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;

            fboRenderer.drawScene(
                    state.sceneX(), state.sceneY(),
                    state.sceneWidth(), state.sceneHeight(),
                    state.mouseX(), state.mouseY()
            );
        } else {
            // Immediate renderer: PIP base class already set output overrides
            // pointing to the PIP's texture. Render the scene directly into it.
            // Use the PIP texture dimensions as viewport (guiSize * guiScale).
            int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            int texWidth = (state.x1() - state.x0()) * guiScale;
            int texHeight = (state.y1() - state.y0()) * guiScale;
            // mouseX/Y from SceneRenderState are GUI logical px in element-local pre-pose space;
            // sceneX/Y is the content origin in the same space. Translate to content-relative,
            // scale to texture px, and flip Y because GL viewport origin is bottom-left while
            // mouse Y is top-down.
            int contentMouseX = (int) ((state.mouseX() - state.sceneX()) * guiScale);
            int contentMouseY = texHeight - (int) ((state.mouseY() - state.sceneY()) * guiScale);
            renderer.renderDirect(texWidth, texHeight, contentMouseX, contentMouseY);
        }
    }

    @Override
    protected void blitTexture(SceneRenderState renderState, GuiRenderState guiRenderState) {
        if (renderState.sceneRenderer() instanceof FBOWorldSceneRenderer fboRenderer) {
            // Blit from FBO's own color texture
            var sceneTexture = fboRenderer.getColorTextureView();
            if (sceneTexture == null) return;

            guiRenderState.addBlitToCurrentLayer(
                    new BlitRenderState(
                            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                            TextureSetup.singleTexture(sceneTexture, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                            renderState.pose(),
                            renderState.x0(),
                            renderState.y0(),
                            renderState.x1(),
                            renderState.y1(),
                            0.0F,
                            1.0F,
                            1.0F,
                            0.0F,
                            -1,
                            renderState.scissorArea(),
                            null
                    )
            );
        } else {
            // Immediate renderer: use default PIP texture blit
            super.blitTexture(renderState, guiRenderState);
        }
    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "scene";
    }

    @Override
    public boolean canBeReusedFor(SceneRenderState state, int textureWidth, int textureHeight) {
        return super.canBeReusedFor(state, textureWidth, textureHeight);
    }
}
