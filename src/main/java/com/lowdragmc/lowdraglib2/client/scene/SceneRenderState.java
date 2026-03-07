package com.lowdragmc.lowdraglib2.client.scene;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * PictureInPicture render state for WorldSceneRenderer.
 * Captures the scene renderer reference and viewport info for deferred rendering.
 * Works for both FBO and Immediate scene renderers.
 */
@OnlyIn(Dist.CLIENT)
public record SceneRenderState(
    WorldSceneRenderer sceneRenderer,
    float sceneX,
    float sceneY,
    float sceneWidth,
    float sceneHeight,
    int mouseX,
    int mouseY,
    Matrix3x2f poseMatrix,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public SceneRenderState(
        WorldSceneRenderer sceneRenderer,
        float sceneX, float sceneY, float sceneWidth, float sceneHeight,
        int mouseX, int mouseY,
        Matrix3x2f poseMatrix,
        int x0, int y0, int x1, int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(sceneRenderer, sceneX, sceneY, sceneWidth, sceneHeight, mouseX, mouseY, poseMatrix,
                x0, y0, x1, y1, scale, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }

    @Override
    public Matrix3x2f pose() {
        return poseMatrix;
    }
}
