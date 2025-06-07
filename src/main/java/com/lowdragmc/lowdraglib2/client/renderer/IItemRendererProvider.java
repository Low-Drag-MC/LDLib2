package com.lowdragmc.lowdraglib2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Author: KilaBash
 * Date: 2022/04/21
 * Description: 
 */
public interface IItemRendererProvider {
    
    /**
     * A switch to disable the deep rendering of the item stack. {@link  com.lowdragmc.lowdraglib2.core.mixins.ItemRendererMixin#injectRenderItem(ItemStack, ItemDisplayContext, boolean, PoseStack, MultiBufferSource, int, int, BakedModel, CallbackInfo)}
     */
    ThreadLocal<Boolean> disabled = ThreadLocal.withInitial(()->false);

    /**
     * Get the renderer for the item stack.
     * @return return null if the item stack does not have a renderer.
     */
    @Nullable
    IRenderer getRenderer(ItemStack stack);
}
