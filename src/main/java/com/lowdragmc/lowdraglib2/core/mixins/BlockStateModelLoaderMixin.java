package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.lowdragmc.lowdraglib2.client.renderer.IBlockRendererProvider;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockStateModelLoader.class)
public class BlockStateModelLoaderMixin {

    @WrapOperation(method = "lambda$loadBlockStateDefinitions$10",
                   at = @At(value = "INVOKE",
                            target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
                            remap = false, ordinal = 0))
    private void ldlib2$injectStateToModelLocation(Logger instance, String s, Object arg1, Object arg2, Operation<Void> original,
                                                  final @Local(ordinal = 0, argsOnly = true) ModelResourceLocation modelResourceLocation,
                                                  @Local(ordinal = 0, argsOnly = true) BlockState blockState) {
        if (blockState.getBlock() instanceof IBlockRendererProvider) {
            return;
        }
        original.call(instance, s, arg1, arg2);
    }
}
