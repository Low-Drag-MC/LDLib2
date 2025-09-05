package com.lowdragmc.lowdraglib2.editor.resource;

public record BuiltinPath(String name) implements IResourcePath {
    @Override
    public ResourceProviderType getType() {
        return BuiltinResourceProvider.TYPE;
    }

    @Override
    public String getPath() {
        return name;
    }

    @Override
    public String getResourceName() {
        return name;
    }
}
