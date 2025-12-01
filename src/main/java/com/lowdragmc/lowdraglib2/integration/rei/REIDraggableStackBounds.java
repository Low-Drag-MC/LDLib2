package com.lowdragmc.lowdraglib2.integration.rei;

import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public record REIDraggableStackBounds(DraggingContext<Screen> context, DraggableStack stack, List<DraggableStackVisitor.BoundsProvider> boundsProviders) {
}
