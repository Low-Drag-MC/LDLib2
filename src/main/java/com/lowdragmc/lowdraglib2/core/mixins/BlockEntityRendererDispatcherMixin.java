package com.lowdragmc.lowdraglib2.core.mixins;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRendererDispatcherMixin {

    @Inject(method = "getRenderer(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;", at = @At(value = "RETURN"), cancellable = true)
    private <T extends BlockEntity, S extends BlockEntityRenderState>
    void ldlib2$getRenderer(S state, CallbackInfoReturnable<BlockEntityRenderer<T, S>> cir) {
        BlockEntityRenderer<T, S> renderer = cir.getReturnValue();
        // todo renderer
//        if (renderer instanceof ATESRRendererProvider && !((ATESRRendererProvider<T>) renderer).hasRenderer(pBlockEntity)) {
//            cir.setReturnValue(null);
//        }
    }

    @Inject(method = "getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;", at = @At(value = "RETURN"), cancellable = true)
    private <T extends BlockEntity, S extends BlockEntityRenderState>
    void ldlib2$getRenderer(T blockEntity, CallbackInfoReturnable<BlockEntityRenderer<T, S>> cir) {
        BlockEntityRenderer<T, S> renderer = cir.getReturnValue();
        // todo renderer
//        if (renderer instanceof ATESRRendererProvider && !((ATESRRendererProvider<T>) renderer).hasRenderer(pBlockEntity)) {
//            cir.setReturnValue(null);
//        }
    }

}
