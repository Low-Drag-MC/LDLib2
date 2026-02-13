package com.lowdragmc.lowdraglib2.gui.ui.data;

import dev.vfyjxf.taffy.style.TrackSizingFunction;

import java.util.List;

public record GridAuto(List<TrackSizingFunction> values) {
    public static final GridAuto EMPTY = new GridAuto(List.of());
}
