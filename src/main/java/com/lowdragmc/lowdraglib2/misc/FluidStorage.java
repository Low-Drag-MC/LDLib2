package com.lowdragmc.lowdraglib2.misc;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.Predicate;

public class FluidStorage extends FluidTank implements IFluidHandlerModifiable, IContentChangeAware {
    @Getter
    @Setter
    private Runnable onContentsChanged = Runnables.doNothing();


    public FluidStorage(int capacity) {
        super(capacity);
    }

    public FluidStorage(int capacity, Predicate<FluidStack> validator) {
        super(capacity, validator);
    }

    @Override
    public void setFluidInTank(int tank, FluidStack fluid) {
        this.fluid = fluid;
        onContentsChanged();
    }

    public void setFluidInTank(FluidStack fluid, boolean notify) {
        this.fluid = fluid;
        if (notify) {
            onContentsChanged();
        }
    }

    public void onContentsChanged() {
        onContentsChanged.run();
    }

    public FluidStorage copy() {
        var storage = new FluidStorage(capacity, validator);
        storage.setFluid(fluid.copy());
        return storage;
    }

}
