package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.client.renderer.ATESRRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRendererDispatcherMixin {

    @Inject(method = "getRenderer", at = @At(value = "RETURN"), cancellable = true)
    private <T extends BlockEntity> void injectGetRenderer(T pBlockEntity, CallbackInfoReturnable<BlockEntityRenderer<T>> cir) {
        BlockEntityRenderer<T> renderer = cir.getReturnValue();
        if (renderer instanceof ATESRRendererProvider && !((ATESRRendererProvider<T>) renderer).hasRenderer(pBlockEntity)) {
            cir.setReturnValue(null);
        }
    }

}
