package com.lowdragmc.lowdraglib2.gui.ui.style.value;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TooltipsValue extends StyleValue<List<Component>> {

    public TooltipsValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable List<Component> doCompute(String rawValue) {
        return List.of(Component.translatable(rawValue));
    }
}
