package com.lowdragmc.lowdraglib2.misc;

import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CycleFluidTransfer implements IFluidHandlerModifiable {
    private List<List<FluidStack>> stacks;

    public CycleFluidTransfer(List<List<FluidStack>> stacks) {
        this.updateStacks(stacks);
    }

    public void updateStacks(List<List<FluidStack>> stacks) {
        this.stacks = stacks;
    }

    @Override
    public int getTanks() {
        return this.stacks.size();
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        List<FluidStack> stackList = this.stacks.get(tank);
        return stackList != null && !stackList.isEmpty() ? stackList.get(Math.abs((int)(System.currentTimeMillis() / 1000L) % stackList.size())) : FluidStack.EMPTY;
    }

    @Override
    public void setFluidInTank(int tank, @NotNull FluidStack fluidStack) {
        if (tank >= 0 && tank < this.stacks.size()) {
            this.stacks.set(tank, List.of(fluidStack));
        }
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.getFluidInTank(tank).getAmount();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public boolean supportsFill(int tank) {
        return false;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public boolean supportsDrain(int tank) {
        return false;
    }

    public List<FluidStack> getStackList(int i) {
        return this.stacks.get(i);
    }
}
