package com.lowdragmc.lowdraglib2.misc;

import com.lowdragmc.lowdraglib2.LDLib2;
import lombok.Setter;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author KilaBash
 * @date 2023/2/25
 * @implNote FluidTransferList
 */
@ParametersAreNonnullByDefault
public class FluidTransferList implements IFluidHandlerModifiable, ValueIOSerializable {
    public final IFluidHandler[] transfers;
    @Setter
    protected Predicate<FluidStack> filter = fluid -> true;

    public FluidTransferList(IFluidHandler... transfers) {
        this.transfers = transfers;
    }

    public FluidTransferList(List<IFluidHandler> transfers) {
        this.transfers = transfers.toArray(IFluidHandler[]::new);
    }

    @Override
    public int getTanks() {
        return Arrays.stream(transfers).mapToInt(IFluidHandler::getTanks).sum();
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        int index = 0;
        for (var transfer : transfers) {
            if (tank - index < transfer.getTanks()) {
                return transfer.getFluidInTank(tank - index);
            }
            index += transfer.getTanks();
        }
        return FluidStack.EMPTY;
    }

    @Override
    public void setFluidInTank(int tank, @NotNull FluidStack fluidStack) {
        int index = 0;
        for (IFluidHandler transfer : transfers) {
            if (transfer instanceof IFluidHandlerModifiable modifiable) {
                if (tank - index < transfer.getTanks()) {
                    modifiable.setFluidInTank(tank - index, fluidStack);
                    return;
                }
            }
            index += transfer.getTanks();
        }
    }

    @Override
    public int getTankCapacity(int tank) {
        int index = 0;
        for (IFluidHandler transfer : transfers) {
            if (tank - index < transfer.getTanks()) {
                return transfer.getTankCapacity(tank - index);
            }
            index += transfer.getTanks();
        }
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        if (!filter.test(stack)) {
            return false;
        }
        int index = 0;
        for (IFluidHandler transfer : transfers) {
            if (tank - index < transfer.getTanks()) {
                return transfer.isFluidValid(tank - index, stack);
            }
            index += transfer.getTanks();
        }
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !filter.test(resource)) return 0;
        var copied = resource.copy();
        for (var transfer : transfers) {
            var candidate = copied.copy();
            copied.shrink(transfer.fill(candidate, action));
            if (copied.isEmpty()) break;
        }
        return resource.getAmount() - copied.getAmount();
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !filter.test(resource)) return FluidStack.EMPTY;
        var copied = resource.copy();
        for (var transfer : transfers) {
            var candidate = copied.copy();
            copied.shrink(transfer.drain(candidate, action).getAmount());
            if (copied.isEmpty()) break;
        }
        copied.setAmount(resource.getAmount() - copied.getAmount());
        return copied;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain == 0) {
            return FluidStack.EMPTY;
        }
        FluidStack totalDrained = null;
        for (var storage : transfers) {
            if (totalDrained == null || totalDrained.isEmpty()) {
                totalDrained = storage.drain(maxDrain, action);
                if (totalDrained.isEmpty()) {
                    totalDrained = null;
                } else {
                    maxDrain -= totalDrained.getAmount();
                }
            } else {
                FluidStack copy = totalDrained.copy();
                copy.setAmount(maxDrain);
                FluidStack drain = storage.drain(copy, action);
                totalDrained.grow(drain.getAmount());
                maxDrain -= drain.getAmount();
            }
            if (maxDrain <= 0) break;
        }
        return totalDrained == null ? FluidStack.EMPTY : totalDrained;
    }

    @Override
    public void serialize(ValueOutput output) {
        var list = output.childrenList("tanks");
        for (var transfer : transfers) {
            if (transfer instanceof ValueIOSerializable serializable) {
                serializable.serialize(list.addChild());
            } else {
                LDLib2.LOGGER.warn("[FluidTransferList] internal tank doesn't support serialization");
            }
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        input.childrenList("tanks").ifPresent(tanks -> {
            var index = 0;
            for (var tank : tanks) {
                if (transfers[index] instanceof ValueIOSerializable serializable) {
                    serializable.deserialize(tank);
                } else {
                    LDLib2.LOGGER.warn("[FluidTransferList] internal tank doesn't support serialization");
                }
                index++;
            }
        });
    }

    @Override
    public boolean supportsFill(int tank) {
        for (var transfer : transfers) {
            if (tank >= transfer.getTanks()) {
                tank -= transfer.getTanks();
                continue;
            }

            if (transfer instanceof IFluidHandlerModifiable modifiable) {
                return modifiable.supportsFill(tank);
            }
        }

        return true;
    }

    @Override
    public boolean supportsDrain(int tank) {
        for (IFluidHandler transfer : transfers) {
            if (tank >= transfer.getTanks()) {
                tank -= transfer.getTanks();
                continue;
            }

            if (transfer instanceof IFluidHandlerModifiable modifiable) {
                return modifiable.supportsDrain(tank);
            }
        }

        return true;
    }
}
