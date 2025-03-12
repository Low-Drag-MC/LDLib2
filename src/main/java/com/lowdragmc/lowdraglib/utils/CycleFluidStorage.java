package com.lowdragmc.lowdraglib.utils;

import com.lowdragmc.lowdraglib.misc.IFluidHandlerModifiable;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CycleFluidStorage implements IFluidTank, IFluidHandlerModifiable {
    private List<FluidStack> storages;
    @Getter @Setter
    private int capacity;

    public CycleFluidStorage(int capacity, List<FluidStack> storages) {
        setCapacity(capacity);
        updateStacks(storages);
    }

    public void updateStacks(List<FluidStack> storages) {
        this.storages = storages;
    }


    @NotNull
    @Override
    public FluidStack getFluid() {
        return storages == null || storages.isEmpty() ? FluidStack.EMPTY : storages.get(Math.abs((int)(System.currentTimeMillis() / 1000) % storages.size()));
    }

    @Override
    public int getFluidAmount() {
        return getFluid().getAmount();
    }

    public void setFluid(FluidStack fluid) {
        updateStacks(List.of(fluid));
    }

    @Override
    public void setFluidInTank(int tank, FluidStack stack) {
        setFluid(stack);
    }

    @Override
    public boolean isFluidValid(FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public boolean supportsFill(int tank) {
        return false;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public boolean supportsDrain(int tank) {
        return false;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }
}
