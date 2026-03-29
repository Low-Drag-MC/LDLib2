package com.lowdragmc.lowdraglib2.utils.items;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Fabric proxy for NeoForge IItemHandlerModifiable.
 */
public interface IItemHandlerModifiable extends IItemHandler {

    void setStackInSlot(int slot, @NotNull ItemStack stack);

}
