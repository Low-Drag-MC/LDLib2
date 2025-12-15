package com.lowdragmc.lowdraglib2.integration.xei.rei.handler;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;

import java.util.ArrayList;
import java.util.List;

public final class REIRecipeWidgetHandler {
    public final Rectangle containerBounds;
    public final List<Widget> slots = new ArrayList<>();

    public REIRecipeWidgetHandler(Rectangle containerBounds) {
        this.containerBounds = containerBounds;
    }

    public void addWidget(Widget slot) {
        slots.add(slot);
    }
}
