package com.lowdragmc.lowdraglib.side.fluid.fabric;

import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.side.item.fabric.ItemTransferHelperImpl;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * @author KilaBash
 * @date 2023/2/10
 * @implNote FluidTransferHelper
 */
@SuppressWarnings("UnstableApiUsage")
public class FluidTransferHelperImpl extends FluidTransferHelper {

    public static Storage<FluidVariant> toFluidVariantStorage(IFluidTransfer fluidTransfer) {
        return new FluidTransferProxyStorage(fluidTransfer);
    }


    public static IFluidTransfer toFluidTransfer(Storage<FluidVariant> storage) {
        return new FluidStorageProxyFluidTransfer(storage);
    }

    public static IFluidTransfer getFluidTransfer(Level level, BlockPos pos, @Nullable Direction direction) {
        var storage = FluidStorage.SIDED.find(level, pos, direction);
        return storage == null ?  null : toFluidTransfer(storage);
    }

    public static IFluidTransfer getFluidTransfer(IItemTransfer itemTransfer, int slot) {
        var handContext = ContainerItemContext.ofSingleSlot(ItemTransferHelperImpl.toSingleStackStorage(itemTransfer, slot));
        var storage = handContext.find(FluidStorage.ITEM);
        return storage == null ?  null : toFluidTransfer(storage);
    }

    public static IFluidTransfer getFluidTransfer(Player player, AbstractContainerMenu screenHandler) {
        var handContext = ContainerItemContext.ofPlayerCursor(player, screenHandler);
        var storage = handContext.find(FluidStorage.ITEM);
        return storage == null ?  null : toFluidTransfer(storage);
    }

    public static IFluidTransfer getFluidTransfer(Player player, InteractionHand hand) {
        var handContext = ContainerItemContext.ofPlayerHand(player, hand);
        var storage = handContext.find(FluidStorage.ITEM);
        return storage == null ?  null : toFluidTransfer(storage);
    }

    public static IFluidTransfer getFluidTransfer(Player player, int slot) {
        var handContext = ContainerItemContext.ofPlayerSlot(player, PlayerInventoryStorage.of(player).getSlot(slot));
        var storage = handContext.find(FluidStorage.ITEM);
        return storage == null ?  null : toFluidTransfer(storage);
    }

    public static ItemStack getContainerItem(ItemStackTransfer copyContainer, IFluidTransfer handler) {
        return copyContainer.getStackInSlot(0);
    }

    public static void exportToTarget(IFluidTransfer source, int maxAmount, Predicate<FluidStack> filter, Level level, BlockPos pos, @Nullable Direction direction) {
        var target = FluidStorage.SIDED.find(level, pos, direction);
        if (target != null) {
            StorageUtil.move(toFluidVariantStorage(source), target, variant -> {
                if (filter == null) return true;
                return filter.test(FluidStack.create(variant.getFluid(), 1000, variant.getNbt()));
            }, maxAmount, null);
        }
    }

    public static void importToTarget(IFluidTransfer target, int maxAmount, Predicate<FluidStack> filter, Level level, BlockPos pos, @Nullable Direction direction) {
        var source = FluidStorage.SIDED.find(level, pos, direction);
        if (source != null) {
            StorageUtil.move(source, toFluidVariantStorage(target), variant -> {
                if (filter == null) return true;
                return filter.test(FluidStack.create(variant.getFluid(), 1000, variant.getNbt()));
            }, maxAmount, null);
        }
    }

}
