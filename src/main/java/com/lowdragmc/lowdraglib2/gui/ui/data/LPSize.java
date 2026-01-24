package com.lowdragmc.lowdraglib2.gui.ui.data;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;

public record LPSize(TaffySize<LengthPercentage> size) {
    public static final LPSize ZERO = new LPSize(TaffySize.all(LengthPercentage.ZERO));
}
