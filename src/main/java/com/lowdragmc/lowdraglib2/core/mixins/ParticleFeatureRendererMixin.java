package com.lowdragmc.lowdraglib2.core.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Vanilla {@code ParticleFeatureRenderer.render} reads color/depth attachments straight from
 * {@code Minecraft.getMainRenderTarget()} / {@code levelRenderer.getParticlesTarget()} and
 * ignores {@link RenderSystem#outputColorTextureOverride}. Without this fix, scene-preview
 * particles would draw into the main framebuffer instead of the PIP texture.
 * <p>
 * We hook the single {@code createRenderPass(...)} call (rather than the two
 * {@code getColorTextureView()} call sites in a ternary, which {@code @Redirect} doesn't
 * disambiguate cleanly) and substitute the texture-view arguments when an override is set.
 */
@Mixin(ParticleFeatureRenderer.class)
public class ParticleFeatureRendererMixin {

    @ModifyArgs(
            method = "render(Lnet/minecraft/client/renderer/SubmitNodeCollection;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;"
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
