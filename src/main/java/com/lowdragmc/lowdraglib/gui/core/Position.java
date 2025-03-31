package com.lowdragmc.lowdraglib.gui.core;

import lombok.Data;

@Data(staticConstructor = "of")
public class Position {
    public enum Type { ABSOLUTE, RELATIVE }

    public final Type type;
    public final Dimension left;
    public final Dimension top;
    public final Dimension right;
    public final Dimension bottom;
}
