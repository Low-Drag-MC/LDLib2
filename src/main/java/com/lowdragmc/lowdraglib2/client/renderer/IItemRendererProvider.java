package com.lowdragmc.lowdraglib2.client.renderer;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * Author: KilaBash
 * Date: 2022/04/21
 * Description: 
 */
public interface IItemRendererProvider {
    
    /**
     * A switch to disable the deep rendering of the item stack.
     */
    ThreadLocal<Boolean> disabled = ThreadLocal.withInitial(()->false);

    /**
     * Get the renderer for the item stack.
     * @return return null if the item stack does not have a renderer.
     */
    @Nullable
    IRenderer getRenderer(ItemStack stack);
}
