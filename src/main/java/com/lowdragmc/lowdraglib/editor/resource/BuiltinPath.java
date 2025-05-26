package com.lowdragmc.lowdraglib.editor.resource;

import java.io.File;
import java.util.Optional;

public record BuiltinPath(String name) implements IResourcePath {
    @Override
    public String getResourceName() {
        return name;
    }

    @Override
    public boolean isBuiltinResource() {
        return true;
    }

    @Override
    public boolean isFileResource() {
        return false;
    }

    @Override
    public Optional<File> toFile() {
        return Optional.empty();
    }

}
