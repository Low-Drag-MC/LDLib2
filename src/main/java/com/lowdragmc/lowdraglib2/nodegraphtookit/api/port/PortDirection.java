package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration representing the direction of a port.
 */
public enum PortDirection implements StringRepresentable {
    NONE(0, "none"),
    INPUT(1, "input"),
    OUTPUT(2, "output");

    public final int mask;
    public final String name;

    PortDirection(int mask, String name) {
        this.mask = mask;
        this.name = name;
    }

    /**
     * Gets the opposite direction.
     *
     * @return the opposite direction
     */
    public PortDirection getOpposite() {
        return switch (this) {
            case INPUT -> OUTPUT;
            case OUTPUT -> INPUT;
            default -> NONE;
        };
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
