package com.lowdragmc.lowdraglib.editor.resource;

public record BuiltinPath(String name) implements IResourcePath {
    @Override
    public String getResourceName() {
        return name;
    }
}
