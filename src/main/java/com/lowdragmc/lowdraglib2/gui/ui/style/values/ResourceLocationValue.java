package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import net.minecraft.resources.Identifier;

public class ResourceLocationValue extends StyleValue<Identifier> {

    public ResourceLocationValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected Identifier doCompute(String rawValue) {
        return Identifier.tryParse(rawValue);
    }
    
}