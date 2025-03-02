package com.lowdragmc.lowdraglib.side.item.fabric;

import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.side.item.ItemTransferHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * @author KilaBash
 * @date 2023/2/10
 * @implNote ItemTransferHelperImpl
 */
@SuppressWarnings("UnstableApiUsage")
public class ItemTransferHelperImpl extends ItemTransferHelper {

    public static Storage<ItemVariant> toItemVariantStorage(IItemTransfer itemTransfer) {
        return new ItemTransferProxyStorage(itemTransfer);
    }

    public static SingleStackStorage toSingleStackStorage(IItemTransfer itemTransfer, int slot) {
        return new SingleStackStorage() {
            @Override
            protected ItemStack getStack() {
                return itemTransfer.getStackInSlot(slot);
            }

            @Override
            protected void setStack(ItemStack stack) {
                itemTransfer.setStackInSlot(slot, stack);
            }

            @Override
            protected boolean canInsert(ItemVariant itemVariant) {
                return itemTransfer.insertItem(slot, itemVariant.toStack(), true, false).isEmpty();
            }

            @Override
            protected boolean canExtract(ItemVariant itemVariant) {
                return itemVariant.matches(getStack()) && !itemTransfer.extractItem(slot, Integer.MAX_VALUE, true, false).isEmpty();
            }

            @Override
            protected void onFinalCommit() {
                itemTransfer.onContentsChanged();
            }
        };
    }

    public static IItemTransfer toItemTransfer(Storage<ItemVariant> storage) {
        return new ItemStorageProxyItemTransfer(storage);
    }

    public static IItemTransfer getItemTransfer(Level level, BlockPos pos, @Nullable Direction direction) {
        var storage = ItemStorage.SIDED.find(level, pos, direction);
        return storage == null ? null : toItemTransfer(storage);
    }

    public static void exportToTarget(IItemTransfer source, int maxAmount, Predicate<ItemStack> predicate, Level level, BlockPos pos, @Nullable Direction direction) {
        var target = ItemStorage.SIDED.find(level, pos, direction);
        if (target != null) {
            StorageUtil.move(toItemVariantStorage(source), target, iv -> {
                if (predicate == null) return true;
                return predicate.test(iv.toStack());
            }, maxAmount, null);
        }
    }

    public static void importToTarget(IItemTransfer target, int maxAmount, Predicate<ItemStack> predicate, Level level, BlockPos pos, @Nullable Direction direction) {
        var source = ItemStorage.SIDED.find(level, pos, direction);
        if (source != null) {
            StorageUtil.move(source, toItemVariantStorage(target), iv -> {
                if (predicate == null) return true;
                return predicate.test(iv.toStack());
            }, maxAmount, null);
        }
    }
}
