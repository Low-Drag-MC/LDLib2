package com.lowdragmc.lowdraglib2.utils.fluids;

import dev.architectury.fluid.FluidStack;
import org.jetbrains.annotations.NotNull;

/**
 * Fabric proxy for NeoForge IFluidTank.
 */
public interface IFluidTank {

    @NotNull
    FluidStack getFluid();

    int getFluidAmount();

    int getCapacity();

    boolean isFluidValid(FluidStack stack);

    int fill(FluidStack resource, IFluidHandler.FluidAction action);

    @NotNull
    FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action);

    @NotNull
    FluidStack drain(int maxDrain, IFluidHandler.FluidAction action);
}
