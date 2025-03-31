package com.lowdragmc.lowdraglib.gui.core;

import lombok.Data;

@Data(staticConstructor = "of")
public final class Rect {
    public final int x;
    public final int y;
    public final int width;
    public final int height;
}
