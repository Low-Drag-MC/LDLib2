package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationValue extends StyleValue<ResourceLocation> {

    public ResourceLocationValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected ResourceLocation doCompute(String rawValue) {
        return ResourceLocation.tryParse(rawValue);
    }
    
}