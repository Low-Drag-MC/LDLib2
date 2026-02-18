package com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire;

public enum WireSide {
    FROM,
    TO;

    public WireSide getOpposite() {
        return this == FROM ? TO : FROM;
    }
}
