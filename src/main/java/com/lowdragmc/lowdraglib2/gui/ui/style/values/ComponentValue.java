package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import net.minecraft.network.chat.Component;

public class ComponentValue extends StyleValue<Component> {

    public ComponentValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected Component doCompute(String rawValue) {
        return Component.translatable(rawValue);
    }
    
}