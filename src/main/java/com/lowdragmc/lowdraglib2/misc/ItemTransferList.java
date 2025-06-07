package com.lowdragmc.lowdraglib2.misc;

import com.lowdragmc.lowdraglib2.LDLib2;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author KilaBash
 * @date 2023/2/25
 * @implNote ItemTransferList
 */
public class ItemTransferList implements IItemHandlerModifiable, INBTSerializable<CompoundTag> {

    public final IItemHandlerModifiable[] transfers;
    @Setter
    protected Predicate<ItemStack> filter = item -> true;

    public ItemTransferList(IItemHandlerModifiable... transfers) {
        this.transfers = transfers;
    }

    public ItemTransferList(List<IItemHandlerModifiable> transfers) {
        this.transfers = transfers.toArray(IItemHandlerModifiable[]::new);
    }

    @Override
    public int getSlots() {
        return Arrays.stream(transfers).mapToInt(IItemHandler::getSlots).sum();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        int index = 0;
        for (var transfer : transfers) {
            if (slot - index < transfer.getSlots()) {
                return transfer.getStackInSlot(slot - index);
            }
            index += transfer.getSlots();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!filter.test(stack)) {
            return stack;
        }
        int index = 0;
        for (var transfer : transfers) {
            if (slot - index < transfer.getSlots()) {
                return transfer.insertItem(slot - index, stack, simulate);
            }
            index += transfer.getSlots();
        }
        return stack;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        int index = 0;
        for (var transfer : transfers) {
            if (slot - index < transfer.getSlots()) {
                return transfer.extractItem(slot - index, amount, simulate);
            }
            index += transfer.getSlots();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        int index = 0;
        for (var transfer : transfers) {
            if (slot - index < transfer.getSlots()) {
                transfer.setStackInSlot(slot - index, stack);
                return;
            }
            index += transfer.getSlots();
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        int index = 0;
        for (var transfer : transfers) {
            if (slot - index < transfer.getSlots()) {
                return transfer.getSlotLimit(slot - index);
            }
            index += transfer.getSlots();
        }
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (!filter.test(stack)) {
            return false;
        }
        int index = 0;
        for (var transfer : transfers) {
            if (slot - index < transfer.getSlots()) {
                return transfer.isItemValid(slot - index, stack);
            }
            index += transfer.getSlots();
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        var list = new ListTag();
        for (var transfer : transfers) {
            if (transfer instanceof INBTSerializable<?> serializable) {
                list.add(serializable.serializeNBT(provider));
            } else {
                LDLib2.LOGGER.warn("[ItemTransferList] internal container doesn't support serialization");
            }
        }
        tag.put("slots", list);
        tag.putByte("type", list.getElementType());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        var list = nbt.getList("slots", nbt.getByte("type"));
        for (int i = 0; i < list.size(); i++) {
            if (transfers[i] instanceof INBTSerializable serializable) {
                serializable.deserializeNBT(provider, list.get(i));
            } else {
                LDLib2.LOGGER.warn("[ItemTransferList] internal container doesn't support serialization");
            }
        }
    }
}
