package com.lowdragmc.lowdraglib2.client.scene;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

/**
 * PictureInPicture renderer for WorldSceneRenderer.
 * Handles both FBO-based and immediate scene renderers:
 * - FBO: renders to its own textures, blits from those
 * - Immediate: renders directly into the PIP-managed textures
 */
@OnlyIn(Dist.CLIENT)
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
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            renderer.renderDirect(texWidth, texHeight, state.mouseX(), state.mouseY());
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    protected void blitTexture(SceneRenderState renderState, GuiRenderState guiRenderState) {
        GpuTextureView sceneTexture;

        if (renderState.sceneRenderer() instanceof FBOWorldSceneRenderer fboRenderer) {
            // Blit from FBO's own color texture
            sceneTexture = fboRenderer.getColorTextureView();
            if (sceneTexture == null) return;
        } else {
            // Immediate renderer: use default PIP texture blit
            super.blitTexture(renderState, guiRenderState);
            return;
        }

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
    }

    @Override
    protected boolean textureIsReadyToBlit(SceneRenderState renderState) {
        return false;
    }

    @Override
    protected String getTextureLabel() {
        return "scene";
    }

    @Override
    public boolean canBeReusedFor(SceneRenderState state, int textureWidth, int textureHeight) {
        return true;
    }
}
