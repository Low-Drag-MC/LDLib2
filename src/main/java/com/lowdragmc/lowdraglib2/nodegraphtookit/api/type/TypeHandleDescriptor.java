package com.lowdragmc.lowdraglib2.nodegraphtookit.api.type;

import java.util.Objects;

public record TypeHandleDescriptor(TypeHandle typeHandle, String friendlyName) {
    public TypeHandleDescriptor(TypeHandle typeHandle, String friendlyName) {
        this.typeHandle = Objects.requireNonNull(typeHandle, "typeHandle");
        this.friendlyName = friendlyName;
    }
}
