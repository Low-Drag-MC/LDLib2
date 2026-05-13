package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3x2f;

/// PIP state carrying a captured GUI subtree to be rendered with group opacity
/// and optional mask. The {@code subState} accumulates all draw calls made
/// between {@code pushVisualLayer}/{@code popVisualLayer} on the parent
/// {@code GUIContext}.
public record VisualLayerPipState(
        GuiRenderState subState,
        int x0,
        int y0,
        int x1,
        int y1,
        float opacity,
        @Nullable IGuiTexture mask,
        /// Screen-space x/y/w/h of the element this layer belongs to.
        /// Used to position the mask inside the off-target so its alpha
        /// punches the element's region only.
        float maskX,
        float maskY,
        float maskW,
        float maskH,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public VisualLayerPipState(GuiRenderState subState, int x0, int y0, int x1, int y1,
                               float opacity, @Nullable IGuiTexture mask,
                               float maskX, float maskY, float maskW, float maskH,
                               Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
        this(subState, x0, y0, x1, y1, opacity, mask, maskX, maskY, maskW, maskH, pose, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }

    @Override
    public float scale() {
        return 1.0f;
    }
}
