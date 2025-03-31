package com.lowdragmc.lowdraglib.gui.core;

import lombok.Data;

@Data(staticConstructor = "of")
public class Flex {
    public final float grow;
    public final float shrink;
    public final Dimension basis;
}
