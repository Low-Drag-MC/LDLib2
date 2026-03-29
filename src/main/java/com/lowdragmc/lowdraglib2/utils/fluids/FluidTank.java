package com.lowdragmc.lowdraglib2.utils.fluids;

import dev.architectury.fluid.FluidStack;
import com.lowdragmc.lowdraglib2.utils.INBTSerializable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Fabric proxy for NeoForge FluidTank.
 */
public class FluidTank implements IFluidHandler, IFluidHandlerModifiable, IFluidTank, INBTSerializable<CompoundTag> {

    @Getter
    protected Predicate<FluidStack> validator = stack -> true;
    @Getter
    @NotNull
    protected FluidStack fluid = FluidStack.empty();
    @Getter @Setter
    protected int capacity;

    public FluidTank(int capacity) {
        this.capacity = capacity;
    }

    public FluidTank setValidator(Predicate<FluidStack> validator) {
        if (validator != null) {
            this.validator = validator;
        }
        return this;
    }

    public void setFluid(FluidStack stack) {
        this.fluid = stack;
        onContentsChanged();
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    @NotNull
    public FluidStack getFluidInTank(int tank) {
        return getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return isFluidValid(stack);
    }

    @Override
    public boolean isFluidValid(FluidStack stack) {
        return validator.test(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(resource)) {
            return 0;
        }
        if (fluid.isEmpty()) {
            int fillAmount = Math.min(capacity, (int)resource.getAmount());
            if (action.execute()) {
                fluid = resource.copyWithAmount(fillAmount);
                onContentsChanged();
            }
            return fillAmount;
        }
        if (!fluid.isFluidEqual(resource)) {
            return 0;
        }
        int filled = Math.min(capacity - (int)fluid.getAmount(), (int)resource.getAmount());
        if (action.execute()) {
            fluid.grow(filled);
            onContentsChanged();
        }
        return filled;
    }

    @Override
    @NotNull
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !resource.isFluidEqual(fluid)) {
            return FluidStack.empty();
        }
        return drain((int)resource.getAmount(), action);
    }

    @Override
    @NotNull
    public FluidStack drain(int maxDrain, FluidAction action) {
        int drained = Math.min((int)fluid.getAmount(), maxDrain);
        FluidStack stack = fluid.copyWithAmount(drained);
        if (action.execute() && drained > 0) {
            fluid.shrink(drained);
            onContentsChanged();
        }
        return stack;
    }

    @Override
    public void setFluidInTank(int tank, FluidStack stack) {
        setFluid(stack);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        fluid.write(provider, nbt);
        nbt.putInt("Capacity", capacity);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, net.minecraft.nbt.CompoundTag nbt) {
        if (nbt instanceof CompoundTag compound) {
            fluid = FluidStack.read(provider, compound).orElse(dev.architectury.fluid.FluidStack.empty());
            capacity = compound.getInt("Capacity");
        }
    }

    @Override
    public int getFluidAmount() {
        return (int)fluid.getAmount();
    }

    protected void onContentsChanged() {

    }
}
