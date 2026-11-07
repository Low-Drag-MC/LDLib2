package com.lowdragmc.lowdraglib2.misc;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class ItemStackTransfer extends ItemStackHandler implements IContentChangeAware {
    @Getter
    @Setter
    private Runnable onContentsChanged = Runnables.doNothing();

    @Setter
    private Function<ItemStack, Boolean> filter;

    public ItemStackTransfer() {
        this(1);
    }

    public ItemStackTransfer(int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public ItemStackTransfer(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    public ItemStackTransfer(ItemStack stack) {
        this(NonNullList.of(ItemStack.EMPTY, stack));
    }

    public void setStackInSlot(int slot, @Nonnull ItemStack stack, boolean notify) {
        validateSlotIndex(slot);
        this.stacks.set(slot, stack);
        if (notify) {
            onContentsChanged(slot);
        }
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return filter == null || filter.apply(stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        onContentsChanged.run();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var snapshot = stacks;
        var list = new ListTag();
        for (int i = 0; i < snapshot.size(); i++) {
            var stack = snapshot.get(i).copy();
            if (stack.isEmpty()) continue;
            var itemTag = new CompoundTag();
            itemTag.putInt("Slot", i);
            list.add(stack.save(provider, itemTag));
        }
        var tag = new CompoundTag();
        tag.put("Items", list);
        tag.putInt("Size", snapshot.size());
        return tag;
    }

    public ItemStackTransfer copy() {
        var copiedStack = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
        for (int i = 0; i < stacks.size(); i++) {
            copiedStack.set(i, stacks.get(i).copy());
        }
        var copied = new ItemStackTransfer(copiedStack);
        copied.setFilter(filter);
        return copied;
    }
}
