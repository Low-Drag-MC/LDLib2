package com.lowdragmc.lowdraglib2.integration.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;

import java.util.List;

public record EMIDragDropHandlers(EmiIngredient dragged, List<Bounds> bounds) {
}
