package com.lowdragmc.lowdraglib.gui.core;

import lombok.Data;

@Data(staticConstructor = "of")
public class Spacing {
    public static final Spacing ZERO = Spacing.of(0, 0, 0, 0);
    public final int left;
    public final int top;
    public final int right;
    public final int bottom;
}
