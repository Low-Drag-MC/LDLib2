package com.lowdragmc.lowdraglib2.core.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * 26.2: particle rendering moved to {@code QuadParticleFeatureRenderer.executeGroup}, which reads
 * color/depth attachments straight from {@code gameRenderer.mainRenderTarget()} /
 * {@code levelRenderer.particlesTarget()} and ignores {@link RenderSystem#outputColorTextureOverride}.
 * Without this fix, scene-preview particles would draw into the main framebuffer instead of the PIP
 * texture.
 * <p>
 * We hook the single {@code createRenderPass(...)} call and substitute the color (arg 1) / depth
 * (arg 3) texture-view arguments when an override is set.
 */
@Mixin(QuadParticleFeatureRenderer.class)
public class ParticleFeatureRendererMixin {

    @ModifyArgs(
            method = "executeGroup(Lnet/minecraft/client/renderer/feature/FeatureFrameContext;ILjava/util/List;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;"
            )
    )
    private void ldlib2$applyOutputOverride(Args args) {
        var colorOverride = RenderSystem.outputColorTextureOverride;
        var depthOverride = RenderSystem.outputDepthTextureOverride;
        if (colorOverride != null)
            args.set(1, colorOverride);
        if (depthOverride != null)
            args.set(3, depthOverride);
    }
}
