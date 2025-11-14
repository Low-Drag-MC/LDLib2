package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class TooltipsValue extends StyleValue<Tooltips> {

    public TooltipsValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable Tooltips doCompute(String rawValue) {
        return Tooltips.of(Component.translatable(rawValue));
    }
}
