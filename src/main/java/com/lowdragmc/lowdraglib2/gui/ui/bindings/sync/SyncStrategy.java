package com.lowdragmc.lowdraglib2.gui.ui.bindings.sync;

public enum SyncStrategy {
    /**
     * It won't sync the value at all.
     */
    NONE,
    /**
     * It will sync the value immediately when it changes.
     */
    IMMEDIATE,
    /**
     * It will sync the value periodically when it changes, based on a defined interval.
     * The interval can be set using a method in the binding. per tick by default.
     */
    PERIODIC,
    /**
     * It will always sync the value periodically, regardless of whether it has changed or not.
     * Do not use this unless you want to force sync the value every tick.
     */
    ALWAYS;
}
