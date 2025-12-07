package com.lowdragmc.lowdraglib2.integration.xei.rei.handler;

import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class REIDraggableStackBoundsHandler {
    public final DraggingContext<Screen> context;
    public final DraggableStack stack;
    public final List<DraggableStackVisitor.BoundsProvider> boundsProviders;

    public REIDraggableStackBoundsHandler(DraggingContext<Screen> context, DraggableStack stack, List<DraggableStackVisitor.BoundsProvider> boundsProviders) {
        this.context = context;
        this.stack = stack;
        this.boundsProviders = boundsProviders;
    }
}
