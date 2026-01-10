package com.lowdragmc.lowdraglib2.integration.xei.rei.handler;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class REIRecipeWidgetHandler {
    public final Rectangle containerBounds;
    public final Supplier<Matrix4f> localToWorld;
    public final List<Widget> slots = new ArrayList<>();

    public REIRecipeWidgetHandler(Rectangle containerBounds, Supplier<Matrix4f> localToWorld) {
        this.containerBounds = containerBounds;
        this.localToWorld = localToWorld;
    }

    public void addWidget(Widget slot) {
        slots.add(slot);
    }
}
