package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.GameRendererAccessor;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.PictureInPictureRendererAccessor;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import org.joml.Matrix3x2f;
import org.joml.Vector4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class VisualLayerPipRenderer extends PictureInPictureRenderer<VisualLayerPipState> {

    private GuiRenderer subRenderer;
    private final VisualLayerRenderTarget targetWrapper = new VisualLayerRenderTarget();

    // Mask off-target: used when state.mask() != null. Renders the mask IGuiTexture
    // into a separate FBO, then composited into the subtree off-target via
    // MASK_ALPHA_MULTIPLY pipeline (dst.rgba *= mask factor).
    private GpuTexture maskColorTex;
    private GpuTextureView maskColorView;
    private GpuTexture maskDepthTex;
    private GpuTextureView maskDepthView;
    private final VisualLayerRenderTarget maskTargetWrapper = new VisualLayerRenderTarget();

    private boolean inUse;

    public VisualLayerPipRenderer() {
        // 26.2: PictureInPictureRenderer is no-arg (the MultiBufferSource model was removed).
        super();
    }

    @Override
    public Class<VisualLayerPipState> getRenderStateClass() {
        return VisualLayerPipState.class;
    }

    @Override
    public boolean canBeReusedFor(VisualLayerPipState state, int textureWidth, int textureHeight) {
        if (inUse) return false;
        if (state.dynamicMask()) return false;
        return super.canBeReusedFor(state, textureWidth, textureHeight);
    }

    private GuiRenderer ensureSubRenderer() {
        if (subRenderer == null) {
            var mainRenderer = ((GameRendererAccessor)(Object) Minecraft.getInstance().gameRenderer).ldlib2$getGuiRenderer();
            var mainExt = (IGuiRendererExt)(Object) mainRenderer;
            subRenderer = new GuiRenderer(
                    new GuiRenderState(),
                    mainExt.ldlib2$getFeatureRenderDispatcher(),
                    List.of(new PictureInPictureRendererRegistration<>(
                            VisualLayerPipState.class, VisualLayerPipRenderer::new))
            );
        }
        return subRenderer;
    }

    private void ensureMaskTextures(int width, int height) {
        if (maskColorTex != null && maskColorTex.getWidth(0) == width && maskColorTex.getHeight(0) == height) {
            return;
        }
        closeMaskTextures();
        var device = RenderSystem.getDevice();
        maskColorTex = device.createTexture(() -> "ldlib2 visual-layer mask color", 13, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
        maskColorView = device.createTextureView(maskColorTex);
        var depthFormat = Minecraft.getInstance().gameRenderer.mainRenderTarget().getDepthTexture().getFormat();
        maskDepthTex = device.createTexture(() -> "ldlib2 visual-layer mask depth", 9, depthFormat, width, height, 1, 1);
        maskDepthView = device.createTextureView(maskDepthTex);
    }

    private void closeMaskTextures() {
        if (maskColorTex != null) { maskColorTex.close(); maskColorTex = null; }
        if (maskColorView != null) { maskColorView.close(); maskColorView = null; }
        if (maskDepthTex != null) { maskDepthTex.close(); maskDepthTex = null; }
        if (maskDepthView != null) { maskDepthView.close(); maskDepthView = null; }
    }

    @Override
    public void close() {
        super.close();
        closeMaskTextures();
    }

    @Override
    protected void renderToTexture(VisualLayerPipState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        GpuTextureView subColorView = RenderSystem.outputColorTextureOverride;
        GpuTextureView subDepthView = RenderSystem.outputDepthTextureOverride;
        if (subColorView == null) return;

        int width = subColorView.getWidth(0);
        int height = subColorView.getHeight(0);
        var sub = ensureSubRenderer();
        var subExt = (IGuiRendererExt)(Object) sub;

        inUse = true;
        try {
            // PASS 1: subtree → subtree off-target
            targetWrapper.bind(subColorView, subColorView.texture(), subDepthView,
                    subDepthView != null ? subDepthView.texture() : null, width, height);
            subExt.ldlib2$setRenderState(state.subState());
            IGuiRendererExt.ldlib2$pushTargetOverride(targetWrapper);
            try {
                sub.render();
            } finally {
                IGuiRendererExt.ldlib2$popTargetOverride();
                targetWrapper.unbind();
            }

            // PASSES 2 & 3: mask → mask off-target, then alpha-multiply into subtree off-target
            if (state.mask() != null) {
                renderMaskAndComposite(state, sub, subExt, width, height,
                        subColorView, subDepthView);
            }
        } finally {
            inUse = false;
        }
    }

    private void renderMaskAndComposite(
            VisualLayerPipState state, GuiRenderer sub, IGuiRendererExt subExt,
            int width, int height, GpuTextureView subColorView, GpuTextureView subDepthView) {

        var mc = Minecraft.getInstance();
        ensureMaskTextures(width, height);
        RenderSystem.getDevice().createCommandEncoder()
                .clearColorAndDepthTextures(maskColorTex, new Vector4f(), maskDepthTex, 0.0);

        // PASS 2: record mask draw into a fresh sub-state, then flush to mask off-target.
        // Use GUIContext.of so all texture-renderer registries are initialized.
        var maskState = new GuiRenderState();
        var maskGraphics = new GuiGraphicsExtractor(mc, maskState, 0, 0);
        var maskCtx = GUIContext.of(maskGraphics, 0, 0, 0);
        maskCtx.pose.pose.set(state.maskPose());
        maskCtx.drawTexture(state.mask(), state.maskX(), state.maskY(), state.maskW(), state.maskH());

        maskTargetWrapper.bind(maskColorView, maskColorTex, maskDepthView, maskDepthTex, width, height);
        RenderSystem.outputColorTextureOverride = maskColorView;
        RenderSystem.outputDepthTextureOverride = maskDepthView;
        subExt.ldlib2$setRenderState(maskState);
        IGuiRendererExt.ldlib2$pushTargetOverride(maskTargetWrapper);
        try {
            sub.render();
        } finally {
            IGuiRendererExt.ldlib2$popTargetOverride();
            maskTargetWrapper.unbind();
        }

        // PASS 3: full-screen quad with MASK_ALPHA_MULTIPLY pipeline, sampling the
        // mask off-target. The blend func (ZERO, SRC_ALPHA, ZERO, SRC_ALPHA)
        // multiplies subtree off-target color and alpha by the mask factor.
        var compositeState = new GuiRenderState();
        compositeState.addGuiElement(new BlitRenderState(
                LDLibRenderPipelines.MASK_ALPHA_MULTIPLY,
                TextureSetup.singleTexture(maskColorView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                new Matrix3x2f(),
                0, 0, state.x1(), state.y1(),
                // GL-flipped V: off-targets store y bottom-up. Same convention as
                // vanilla PictureInPictureRenderer.blitTexture (v0=1, v1=0).
                0.0F, 1.0F, 1.0F, 0.0F,
                -1,
                null
        ));

        // Switch outputs back to subtree off-target
        RenderSystem.outputColorTextureOverride = subColorView;
        RenderSystem.outputDepthTextureOverride = subDepthView;
        subExt.ldlib2$setRenderState(compositeState);
        IGuiRendererExt.ldlib2$pushTargetOverride(targetWrapper);
        targetWrapper.bind(subColorView, subColorView.texture(), subDepthView,
                subDepthView != null ? subDepthView.texture() : null, width, height);
        try {
            sub.render();
        } finally {
            IGuiRendererExt.ldlib2$popTargetOverride();
            targetWrapper.unbind();
        }
    }

    @Override
    protected void blitTexture(VisualLayerPipState state, GuiRenderState guiRenderState) {
        // GUI_TEXTURED_PREMULTIPLIED_ALPHA blend = (ONE, 1-srcA) — expects premultiplied.
        // Off-target is straight-alpha, so we premultiply opacity into rgb here:
        //   tint = (opacity, opacity, opacity, opacity)
        // shader does sample * tint, output is premultiplied.
        GpuTextureView view = ((PictureInPictureRendererAccessor)(Object) this).ldlib2$getTextureView();
        int alpha = Math.round(Mth.clamp(state.opacity(), 0.0F, 1.0F) * 255.0F);
        int tint = ARGB.color(alpha, alpha, alpha, alpha);
        guiRenderState.addBlitToCurrentLayer(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(),
                state.x0(), state.y0(), state.x1(), state.y1(),
                0.0F, 1.0F, 1.0F, 0.0F,
                tint,
                state.scissorArea()
        ));
    }

    @Override
    protected String getTextureLabel() {
        return "visual-layer";
    }
}
