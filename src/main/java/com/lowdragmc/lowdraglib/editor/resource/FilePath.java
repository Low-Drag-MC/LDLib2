package com.lowdragmc.lowdraglib.editor.resource;

import java.io.File;

public record FilePath(File file) implements IResourcePath {

    @Override
    public String getResourceName() {
        var name = file.getName();
        var dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1) ? name : name.substring(0, dotIndex);
    }

}
