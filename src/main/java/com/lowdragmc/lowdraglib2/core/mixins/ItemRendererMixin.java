package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    public void injectRenderItem(ItemStack stack, 
                                 ItemDisplayContext itemDisplayContext, 
                                 boolean leftHanded, 
                                 PoseStack matrices, 
                                 MultiBufferSource buffer,
                                 int light, int overlay, 
                                 BakedModel model,
                                 CallbackInfo ci){
        if (stack.getItem() instanceof IItemRendererProvider && !IItemRendererProvider.disabled.get()) {
            IRenderer renderer =((IItemRendererProvider) stack.getItem()).getRenderer(stack);
            if (renderer != null) {
                if (itemDisplayContext == ItemDisplayContext.GUI) {
                    if (renderer.useBlockLight(stack) != model.usesBlockLight()) {
                        if (renderer.useBlockLight(stack)) {
                            Lighting.setupFor3DItems();
                        } else {
                            Lighting.setupForFlatItems();
                        }
                    }
                }
                renderer.renderItem(stack, itemDisplayContext, leftHanded, matrices, buffer, light, overlay, model);
                if (itemDisplayContext == ItemDisplayContext.GUI) {
                    if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
                        bufferSource.endBatch();
                        if (model.usesBlockLight()) {
                            Lighting.setupFor3DItems();
                        } else {
                            Lighting.setupForFlatItems();
                        }
                    }
                }
                ci.cancel();
            }
        }
    }

}
