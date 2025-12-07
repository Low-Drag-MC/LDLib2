package com.lowdragmc.lowdraglib2.integration.xei.emi.handler;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

public final class EMIDragDropHandler {
    public final EmiIngredient dragged;
    public final List<Bounds> bounds;

    public EMIDragDropHandler(EmiIngredient dragged) {
        this.dragged = dragged;
        this.bounds = Lists.newArrayList();
    }

}
