package com.lowdragmc.lowdraglib2.gui.slot;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;

public class LocalSlot extends Slot {
    public LocalSlot() {
        super(new SimpleContainer(1), 0, 0, 0);
    }
}
