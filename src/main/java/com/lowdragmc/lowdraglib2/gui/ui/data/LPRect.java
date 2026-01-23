package com.lowdragmc.lowdraglib2.gui.ui.data;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;

public record LPRect(TaffyRect<LengthPercentage> rect) {
    public static final LPRect ZERO = new LPRect(TaffyRect.all(LengthPercentage.ZERO));
}
