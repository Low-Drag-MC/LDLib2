package com.lowdragmc.lowdraglib2.utils.fluids;

import dev.architectury.fluid.FluidStack;

/**
 * Fabric proxy for NeoForge IFluidHandlerModifiable.
 */
public interface IFluidHandlerModifiable extends IFluidHandler {

    void setFluidInTank(int tank, FluidStack stack);

    default boolean supportsFill(int tank) {
        return true;
    }

    default boolean supportsDrain(int tank) {
        return true;
    }

}
