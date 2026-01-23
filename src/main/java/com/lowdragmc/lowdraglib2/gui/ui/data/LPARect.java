package com.lowdragmc.lowdraglib2.gui.ui.data;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;

public record LPARect(TaffyRect<LengthPercentageAuto> rect) {
    public static final LPARect ZERO = new LPARect(TaffyRect.all(LengthPercentageAuto.ZERO));
}
