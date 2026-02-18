package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import java.util.Objects;

public final class Capabilities {
    private final String id;

    Capabilities(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public String getId() {
        return id;
    }

    public static final Capabilities RENAMABLE = new Capabilities("Renamable");
    public static final Capabilities DELETABLE = new Capabilities("Deletable");
    public static final Capabilities MOVABLE = new Capabilities("Movable");
    public static final Capabilities SELECTABLE = new Capabilities("Selectable");
    public static final Capabilities COLLAPSIBLE = new Capabilities("Collapsible");
    public static final Capabilities RESIZABLE = new Capabilities("Resizable");
    public static final Capabilities DROPPABLE = new Capabilities("Droppable");
    public static final Capabilities COPIABLE = new Capabilities("Copiable");
    public static final Capabilities COLORABLE = new Capabilities("Colorable");
    public static final Capabilities ASCENDABLE = new Capabilities("Ascendable");
    public static final Capabilities NEEDS_CONTAINER = new Capabilities("NeedsContainer");
    public static final Capabilities DISABLEABLE = new Capabilities("Disableable");

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Capabilities other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Capabilities(" + id + ")";
    }
}
