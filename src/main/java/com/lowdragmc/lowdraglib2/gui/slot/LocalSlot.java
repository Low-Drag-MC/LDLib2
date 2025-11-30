package com.lowdragmc.lowdraglib2.gui.slot;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;

@KJSBindings
public class LocalSlot extends Slot {
    public LocalSlot() {
        super(new SimpleContainer(1), 0, 0, 0);
    }
}
