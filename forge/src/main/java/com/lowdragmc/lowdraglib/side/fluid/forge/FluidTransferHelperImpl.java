package com.lowdragmc.lowdraglib.side.fluid.forge;

import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * @author KilaBash
 * @date 2023/2/10
 * @implNote FluidTransferHelper
 */
public class FluidTransferHelperImpl extends FluidTransferHelper {

    public static IFluidHandler toFluidHandler(IFluidTransfer fluidTransfer) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return fluidTransfer.getTanks();
            }

            @Override
            public @NotNull FluidStack getFluidInTank(int slot) {
                return FluidHelperImpl.toFluidStack(fluidTransfer.getFluidInTank(slot));
            }

            @Override
            public int getTankCapacity(int slot) {
                return (int) fluidTransfer.getTankCapacity(slot);
            }

            @Override
            public boolean isFluidValid(int slot, @NotNull FluidStack fluidStack) {
                return fluidTransfer.isFluidValid(slot, FluidHelperImpl.toFluidStack(fluidStack));
            }

            @Override
            public int fill(FluidStack fluidStack, FluidAction fluidAction) {
                return (int) fluidTransfer.fill(FluidHelperImpl.toFluidStack(fluidStack), fluidAction == FluidAction.SIMULATE);
            }

            @Override
            public @NotNull FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
                return FluidHelperImpl.toFluidStack(fluidTransfer.drain(FluidHelperImpl.toFluidStack(fluidStack), fluidAction == FluidAction.SIMULATE));
            }

            @Override
            public @NotNull FluidStack drain(int amount, FluidAction fluidAction) {
                return FluidHelperImpl.toFluidStack(fluidTransfer.drain(amount, fluidAction == FluidAction.SIMULATE));
            }
        };
    }

    public static IFluidTransfer toFluidTransfer(IFluidHandler handler) {
        if (handler instanceof IFluidTransfer fluidTransfer) {
            return fluidTransfer;
        } else {
            return new FluidTransferWrapper(handler);
        }
    }

    public static IFluidTransfer getFluidTransfer(Level level, BlockPos pos, @Nullable Direction direction) {
        BlockState state = level.getBlockState(pos);
        if (state.hasBlockEntity()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var handler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction);
                if (handler.isPresent()) {
                    return toFluidTransfer(handler.orElse(null));
                }
            }
        }
        return null;
    }

    public static IFluidTransfer getFluidTransfer(IItemTransfer itemTransfer, int slot) {
        var itemStack = itemTransfer.getStackInSlot(slot);
        if (!itemStack.isEmpty()) {
            var handler = itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if (handler.isPresent()) {
                return toFluidTransfer(handler.orElse(null));
            }
        }
        return null;
    }

    public static IFluidTransfer getFluidTransfer(Player player, AbstractContainerMenu screenHandler) {
        var itemStack = screenHandler.getCarried();
        if (!itemStack.isEmpty()) {
            var handler = itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if (handler.isPresent()) {
                return toFluidTransfer(handler.orElse(null));
            }
        }
        return null;
    }

    public static IFluidTransfer getFluidTransfer(Player player, InteractionHand hand) {
        var itemStack = player.getItemInHand(hand);
        if (!itemStack.isEmpty()) {
            var handler = itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if (handler.isPresent()) {
                return toFluidTransfer(handler.orElse(null));
            }
        }
        return null;
    }

    public static IFluidTransfer getFluidTransfer(Player player, int slot) {
        var itemStack = player.getInventory().getItem(slot);
        if (!itemStack.isEmpty()) {
            var handler = itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if (handler.isPresent()) {
                return toFluidTransfer(handler.orElse(null));
            }
        }
        return null;
    }

    public static ItemStack getContainerItem(ItemStackTransfer copyContainer, IFluidTransfer handler) {
        if (handler instanceof FluidTransferWrapper wrapper && wrapper.getHandler() instanceof IFluidHandlerItem fluidHandlerItem) {
            return fluidHandlerItem.getContainer();
        }
        return copyContainer.getStackInSlot(0);
    }

    public static void exportToTarget(IFluidTransfer source, int maxAmount, Predicate<com.lowdragmc.lowdraglib.side.fluid.FluidStack> filter, Level level, BlockPos pos, @Nullable Direction direction) {
        BlockState state = level.getBlockState(pos);
        if (state.hasBlockEntity()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var cap = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction).resolve();
                if (cap.isPresent()) {
                    var target = cap.get();
                    for (int srcIndex = 0; srcIndex < source.getTanks(); srcIndex++) {
                        var currentFluid = source.getFluidInTank(srcIndex);
                        if (currentFluid.isEmpty() || !filter.test(currentFluid)) {
                            continue;
                        }

                        var toDrain = currentFluid.copy();
                        toDrain.setAmount(maxAmount);

                        var filled = target.fill(FluidHelperImpl.toFluidStack(source.drain(toDrain, true)), IFluidHandler.FluidAction.SIMULATE);
                        if (filled > 0) {
                            maxAmount -= filled;
                            toDrain = currentFluid.copy();
                            toDrain.setAmount(filled);
                            target.fill(FluidHelperImpl.toFluidStack(source.drain(toDrain, false)), IFluidHandler.FluidAction.EXECUTE);
                        }
                        if (maxAmount <= 0) return;
                    }
                }
            }
        }
    }

    public static void importToTarget(IFluidTransfer target, int maxAmount, Predicate<com.lowdragmc.lowdraglib.side.fluid.FluidStack> filter, Level level, BlockPos pos, @Nullable Direction direction) {
        BlockState state = level.getBlockState(pos);
        if (state.hasBlockEntity()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var cap = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction).resolve();
                if (cap.isPresent()) {
                    var source = cap.get();
                    for (int srcIndex = 0; srcIndex < source.getTanks(); srcIndex++) {
                        var currentFluid = source.getFluidInTank(srcIndex);
                        if (currentFluid.isEmpty() || !filter.test(FluidHelperImpl.toFluidStack(currentFluid))) {
                            continue;
                        }

                        var toDrain = currentFluid.copy();
                        toDrain.setAmount(maxAmount);

                        var filled = target.fill(FluidHelperImpl.toFluidStack(source.drain(toDrain, IFluidHandler.FluidAction.SIMULATE)), true);
                        if (filled > 0) {
                            maxAmount -= filled;
                            toDrain = currentFluid.copy();
                            toDrain.setAmount((int) filled);
                            target.fill(FluidHelperImpl.toFluidStack(source.drain(toDrain, IFluidHandler.FluidAction.EXECUTE)), false);
                        }
                        if (maxAmount <= 0) return;
                    }
                }
            }
        }
    }
}
