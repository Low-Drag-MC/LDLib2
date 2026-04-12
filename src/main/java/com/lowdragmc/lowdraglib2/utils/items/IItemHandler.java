package com.lowdragmc.lowdraglib2.utils.items;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Fabric proxy for NeoForge IItemHandler.
 */
public interface IItemHandler {

    int getSlots();

    @NotNull
    ItemStack getStackInSlot(int slot);

    @NotNull
    ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate);

    @NotNull
    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, @NotNull ItemStack stack);
}
