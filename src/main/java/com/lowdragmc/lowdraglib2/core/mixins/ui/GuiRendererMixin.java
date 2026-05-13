package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGuiRendererExt;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Surgical mixin to support visual-layer sub-renderers:
/// (1) make `renderState` swappable so a sub-GuiRenderer can adopt a captured sub-state
/// (2) redirect main render target inside `draw()` to allow writing into an off-target
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements IGuiRendererExt {
    @Shadow @Final @Mutable private GuiRenderState renderState;
    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;
    @Shadow @Final private SubmitNodeCollector submitNodeCollector;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;

    @Override
    public void ldlib2$setRenderState(GuiRenderState state) {
        this.renderState = state;
    }

    @Override
    public MultiBufferSource.BufferSource ldlib2$getBufferSource() {
        return this.bufferSource;
    }

    @Override
    public SubmitNodeCollector ldlib2$getSubmitNodeCollector() {
        return this.submitNodeCollector;
    }

    @Override
    public FeatureRenderDispatcher ldlib2$getFeatureRenderDispatcher() {
        return this.featureRenderDispatcher;
    }

    @Redirect(
            method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget ldlib2$redirectRenderTarget(Minecraft mc) {
        RenderTarget override = IGuiRendererExt.ldlib2$peekTargetOverride();
        return override != null ? override : mc.getMainRenderTarget();
    }

    /// Cache the latest fog buffer passed to {@code render()} so sub-renderers
    /// (visual layers) can reuse it without needing access to the private
    /// {@code GameRenderer.fogRenderer}. Stored on IGuiRendererExt because
    /// mixins cannot hold public static members.
    @Inject(method = "render", at = @At("HEAD"))
    private void ldlib2$captureFogBuffer(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        IGuiRendererExt.ldlib2$setLastFogBuffer(fogBuffer);
    }
}
