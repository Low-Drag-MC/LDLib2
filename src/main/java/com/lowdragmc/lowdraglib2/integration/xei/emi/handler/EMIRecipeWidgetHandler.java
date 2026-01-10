package com.lowdragmc.lowdraglib2.integration.xei.emi.handler;

import dev.emi.emi.api.widget.Widget;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class EMIRecipeWidgetHandler {
    public final List<Widget> slots = new ArrayList<>();
    public final Supplier<Matrix4f> localToWorld;

    public EMIRecipeWidgetHandler(Supplier<Matrix4f> localToWorld) {
        this.localToWorld = localToWorld;
    }

    public void addWidget(Widget slot) {
        slots.add(slot);
    }
}
