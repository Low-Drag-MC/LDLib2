package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote AbstractContainerScreenMixin
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor ItemStack getDraggingItem();
    @Accessor int getQuickCraftingRemainder();
    @Accessor("isSplittingStack") boolean isSplittingStack();
    @Accessor ItemStack getSnapbackItem();
    @Accessor void setSnapbackItem(ItemStack item);
    @Accessor int getSnapbackStartX();
    @Accessor int getSnapbackStartY();
    @Accessor Slot getSnapbackEnd();
    @Accessor long getSnapbackTime();

}
