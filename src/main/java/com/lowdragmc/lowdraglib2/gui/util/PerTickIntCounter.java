package com.lowdragmc.lowdraglib2.gui.util;

import net.minecraft.world.level.Level;

public class PerTickIntCounter {

    private final int defaultValue;

    private long lastUpdatedWorldTime;

    private int currentValue;

    public PerTickIntCounter(int defaultValue) {
        this.defaultValue = defaultValue;
        this.currentValue = defaultValue;
    }

    private void checkValueState(Level world) {
        long currentWorldTime = world.getGameTime();
        if (currentWorldTime != lastUpdatedWorldTime) {
            this.lastUpdatedWorldTime = currentWorldTime;
            this.currentValue = defaultValue;
        }
    }

    public int get(Level world) {
        checkValueState(world);
        return currentValue;
    }

    public void increment(Level world, int value) {
        checkValueState(world);
        this.currentValue += value;
    }
}
