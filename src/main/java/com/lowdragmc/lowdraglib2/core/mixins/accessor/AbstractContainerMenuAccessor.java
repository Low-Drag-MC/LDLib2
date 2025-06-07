package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote AbstractContainerScreenMixin
 */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Accessor NonNullList<ItemStack> getLastSlots();
    @Accessor NonNullList<ItemStack> getRemoteSlots();

}
