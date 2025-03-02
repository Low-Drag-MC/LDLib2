package com.lowdragmc.lowdraglib.side.item.forge;

import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

import com.lowdragmc.lowdraglib.side.item.ItemTransferHelper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * @author KilaBash
 * @date 2023/2/10
 * @implNote ItemTransferHelperImpl
 */
public class ItemTransferHelperImpl extends ItemTransferHelper {
    public static IItemHandler toItemHandler(IItemTransfer itemTransfer) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return itemTransfer.getSlots();
            }

            @NotNull
            @Override
            public ItemStack getStackInSlot(int slot) {
                return itemTransfer.getStackInSlot(slot);
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return itemTransfer.insertItem(slot, stack, simulate, !simulate);
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return itemTransfer.extractItem(slot, amount, simulate, !simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return itemTransfer.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return itemTransfer.isItemValid(slot, stack);
            }
        };
    }

    public static IItemTransfer toItemTransfer(IItemHandler handler) {
        return new IItemTransfer() {
            @Override
            public int getSlots() {
                return handler.getSlots();
            }

            @NotNull
            @Override
            public ItemStack getStackInSlot(int slot) {
                return handler.getStackInSlot(slot);
            }

            @Override
            public void setStackInSlot(int index, ItemStack stack) {
                if (handler instanceof IItemHandlerModifiable modifiable) {
                    modifiable.setStackInSlot(index, stack);
                } else {
                    IItemTransfer.super.setStackInSlot(index, stack);
                }
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate, boolean notifyChanges) {
                return handler.insertItem(slot, stack, simulate);
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate, boolean notifyChanges) {
                return handler.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return handler.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return handler.isItemValid(slot, stack);
            }

            @NotNull
            @Override
            public Object createSnapshot() {
                return new Object();
            }

            @Override
            public void restoreFromSnapshot(Object snapshot) {

            }
        };
    }

    public static IItemTransfer getItemTransfer(Level level, BlockPos pos, @Nullable Direction direction) {
        if (level.getBlockState(pos).hasBlockEntity()) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var cap = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction).resolve();
                if (cap.isPresent()) {
                    return toItemTransfer(cap.get());
                }
            }
        }
        return null;
    }

    public static void exportToTarget(IItemTransfer source, int maxAmount, Predicate<ItemStack> predicate, Level level, BlockPos pos, @Nullable Direction direction) {
        if (level.getBlockState(pos).hasBlockEntity()) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var cap = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction).resolve();
                if (cap.isPresent()) {
                    var target = cap.get();
                    for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
                        ItemStack sourceStack = source.extractItem(srcIndex, Integer.MAX_VALUE, true, false);
                        if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                            continue;
                        }
                        ItemStack remainder = insertItem(target, sourceStack, true);
                        int amountToInsert = sourceStack.getCount() - remainder.getCount();
                        if (amountToInsert > 0) {
                            sourceStack = source.extractItem(srcIndex, Math.min(maxAmount, amountToInsert), false, true);
                            insertItem(target, sourceStack, false);
                            maxAmount -= Math.min(maxAmount, amountToInsert);
                            if (maxAmount <= 0) return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Inserts items by trying to fill slots with the same item first, and then fill empty slots.
     */
    public static ItemStack insertItem(IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        if (!stack.isStackable()) {
            return insertToEmpty(handler, stack, simulate);
        }

        IntList emptySlots = new IntArrayList();
        int slots = handler.getSlots();

        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                emptySlots.add(i);
            }
            if (ItemHandlerHelper.canItemStacksStackRelaxed(stack, slotStack)) {
                stack = handler.insertItem(i, stack, simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        for (int slot : emptySlots) {
            stack = handler.insertItem(slot, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    /**
     * Only inerts to empty slots. Perfect for not stackable items
     */
    public static ItemStack insertToEmpty(IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                stack = handler.insertItem(i, stack, simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    public static void importToTarget(IItemTransfer target, int maxAmount, Predicate<ItemStack> predicate, Level level, BlockPos pos, @Nullable Direction direction) {
        if (level.getBlockState(pos).hasBlockEntity()) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var cap = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction).resolve();
                if (cap.isPresent()) {
                    var source = cap.get();
                    for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
                        ItemStack sourceStack = source.extractItem(srcIndex, Integer.MAX_VALUE, true);
                        if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                            continue;
                        }
                        ItemStack remainder = insertItem(target, sourceStack, true);
                        int amountToInsert = sourceStack.getCount() - remainder.getCount();
                        if (amountToInsert > 0) {
                            sourceStack = source.extractItem(srcIndex, Math.min(maxAmount, amountToInsert), false);
                            insertItem(target, sourceStack, false);
                            maxAmount -= Math.min(maxAmount, amountToInsert);
                        }
                        if (maxAmount <= 0) return;
                    }
                }
            }
        }
    }

    /**
     * Inserts items by trying to fill slots with the same item first, and then fill empty slots.
     */
    public static ItemStack insertItem(IItemTransfer handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        if (!stack.isStackable()) {
            return insertToEmpty(handler, stack, simulate);
        }

        IntList emptySlots = new IntArrayList();
        int slots = handler.getSlots();

        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                emptySlots.add(i);
            }
            if (ItemHandlerHelper.canItemStacksStackRelaxed(stack, slotStack)) {
                stack = handler.insertItem(i, stack, simulate, !simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        for (int slot : emptySlots) {
            stack = handler.insertItem(slot, stack, simulate, !simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    /**
     * Only inerts to empty slots. Perfect for not stackable items
     */
    public static ItemStack insertToEmpty(IItemTransfer handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                stack = handler.insertItem(i, stack, simulate, !simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }
}
