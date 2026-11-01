package com.lowdragmc.lowdraglib2.misc;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.syncdata.IContentChangeAware;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

    /**
     * The fluid, as {@link FluidTank} saves it, and the capacity, which {@code FluidTank} leaves out: one changed at
     * runtime, by an upgrade say, has to survive a reload and reach the client, and both go through here.
     */
    @Override
    public void serialize(ValueOutput output) {
        super.serialize(output);
        output.putInt("capacity", capacity);
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        capacity = input.getIntOr("capacity", capacity);
    }

    public FluidStorage copy() {
        var storage = new FluidStorage(capacity, validator);
        storage.setFluid(fluid.copy());
        return storage;
    }

}
