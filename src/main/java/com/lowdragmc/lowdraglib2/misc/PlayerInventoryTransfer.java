package com.lowdragmc.lowdraglib2.misc;

import com.lowdragmc.lowdraglib2.utils.items.IItemHandlerModifiable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerInventoryTransfer implements IItemHandlerModifiable {
    private final Inventory inv;

    public PlayerInventoryTransfer(Inventory inv) {
        this.inv = inv;
    }

    @Override
    public int getSlots() {
        return inv.items.size();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return inv.getItem(slot);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        inv.setItem(slot, stack);
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack existing = inv.getItem(slot);
        int limit = Math.min(64, stack.getMaxStackSize());

        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(existing, stack)) return stack;
            limit -= existing.getCount();
        }

        if (limit <= 0) return stack;
        boolean reachedLimit = stack.getCount() > limit;

        if (!simulate) {
            if (existing.isEmpty()) {
                inv.setItem(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
        }

        return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;
        ItemStack existing = inv.getItem(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (existing.getCount() <= toExtract) {
            ItemStack result = existing.copy();
            if (!simulate) {
                inv.setItem(slot, ItemStack.EMPTY);
            }
            return result;
        } else {
            ItemStack result = existing.copyWithCount(toExtract);
            if (!simulate) {
                existing.shrink(toExtract);
            }
            return result;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return inv.canPlaceItem(slot, stack);
    }
}
