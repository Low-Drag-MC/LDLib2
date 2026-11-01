package com.lowdragmc.lowdraglib2.syncdata.mark;

import com.lowdragmc.lowdraglib2.syncdata.utils.StackSnapshots;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * What an {@link ItemStackHandler} saves is its slots and their count, so comparing slots against copies tells the same
 * as comparing saves, for as long as the class saves and hands out its slots the way the base class does.
 */
@SuppressWarnings("removal") // deprecated on 26.1, but still what LDLib2's own ItemStackTransfer extends
final class ItemHandlerStrategy implements IMarkStrategy<ItemStackHandler, ItemStack[]> {
    @Override
    public boolean test(Class<?> type) {
        return ItemStackHandler.class.isAssignableFrom(type)
                && MarkStrategies.isDeclaredBy(type, ItemStackHandler.class, "serialize", ValueOutput.class)
                && MarkStrategies.isDeclaredBy(type, ItemStackHandler.class, "getSlots")
                && MarkStrategies.isDeclaredBy(type, ItemStackHandler.class, "getStackInSlot", int.class);
    }

    @Override
    public ItemStack @NotNull [] obtainManagedMark(@NotNull ItemStackHandler value) {
        var stacks = new ItemStack[value.getSlots()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = StackSnapshots.of(value.getStackInSlot(i));
        }
        return stacks;
    }

    @Override
    public boolean areDifferent(ItemStack @NotNull [] managedMark, @NotNull ItemStackHandler value) {
        if (managedMark.length != value.getSlots()) return true;
        for (int i = 0; i < managedMark.length; i++) {
            if (!ItemStack.matches(managedMark[i], value.getStackInSlot(i))) return true;
        }
        return false;
    }
}
