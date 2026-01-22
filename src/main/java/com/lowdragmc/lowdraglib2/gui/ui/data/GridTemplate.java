package com.lowdragmc.lowdraglib2.gui.ui.data;

import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.TrackSizingFunction;

import java.util.List;

public record GridTemplate(List<TrackSizingFunction> simples,
                           List<GridTemplateComponent> repeats,
                           List<NamedGridLine> names) {

    public GridTemplate(TrackSizingFunction[] simples, GridTemplateComponent[] repeats, NamedGridLine[] names) {
        this(List.of(simples), List.of(repeats), List.of(names));
    }

    public static final GridTemplate EMPTY = new GridTemplate(List.of(), List.of(), List.of());
}
