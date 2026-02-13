package com.lowdragmc.lowdraglib2.configurator.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.appliedenergistics.yoga.YogaEdge;

public class HeaderConfigurator extends Configurator {
    public HeaderConfigurator(Component value, int topMargin) {
        layout(layout -> layout.marginTop(topMargin));
        setLabel(value);
    }

    public HeaderConfigurator(String value, int topMargin) {
        this(Component.translatable(value).withStyle(Style.EMPTY.withBold(true)), topMargin);
    }
}
