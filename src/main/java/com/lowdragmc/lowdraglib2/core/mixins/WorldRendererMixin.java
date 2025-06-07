package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.client.renderer.IBlockRendererProvider;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",  at = @At(value = "HEAD"), cancellable = true)
    private static void mbd2$getLightColor(BlockAndTintGetter pLevel,
                                               BlockState pState, BlockPos pPos,
                                               CallbackInfoReturnable<Integer> cir) {
        if (pState.getBlock() instanceof IBlockRendererProvider) {
            cir.setReturnValue(((IBlockRendererProvider)pState.getBlock()).getLightMap(pLevel, pState, pPos));
        }
    }

}
