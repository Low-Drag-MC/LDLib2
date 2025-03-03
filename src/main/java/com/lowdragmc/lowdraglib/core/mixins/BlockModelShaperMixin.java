package com.lowdragmc.lowdraglib.core.mixins;

import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelShaper.class)
public abstract class BlockModelShaperMixin {
    @Inject(method = "getParticleIcon", at = @At(value = "HEAD"), cancellable = true)
    private void injectGetRenderer(BlockState state, CallbackInfoReturnable<TextureAtlasSprite> cir) {
        if (state.getBlock() instanceof IBlockRendererProvider rendererProvider) {
            var renderer = rendererProvider.getRenderer(state);
            if (renderer != null) {
                cir.setReturnValue(renderer.getParticleTexture());
            }
        }
    }
}
