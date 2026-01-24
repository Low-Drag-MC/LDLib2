package com.lowdragmc.lowdraglib2.gui.ui.data;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;

public record DimensionSize(TaffySize<TaffyDimension> size) {
    public static final DimensionSize AUTO = new DimensionSize(TaffySize.all(TaffyDimension.AUTO));
}
