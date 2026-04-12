package com.lowdragmc.lowdraglib2.utils.fluids;

public enum FluidAction {
    EXECUTE,
    SIMULATE;

    public boolean execute() {
        return this == EXECUTE;
    }

    public boolean simulate() {
        return this == SIMULATE;
    }
}
