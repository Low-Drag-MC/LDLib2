package com.lowdragmc.lowdraglib2.integration.xei.emi.handler;

import dev.emi.emi.api.widget.Widget;

import java.util.ArrayList;
import java.util.List;

public final class EMIRecipeWidgetHandler {
    public final List<Widget> slots = new ArrayList<>();

    public void addWidget(Widget slot) {
        slots.add(slot);
    }
}
