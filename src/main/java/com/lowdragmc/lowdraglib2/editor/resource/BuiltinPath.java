package com.lowdragmc.lowdraglib2.editor.resource;

public record BuiltinPath(String name) implements IResourcePath {
    @Override
    public String getResourceName() {
        return name;
    }
}
