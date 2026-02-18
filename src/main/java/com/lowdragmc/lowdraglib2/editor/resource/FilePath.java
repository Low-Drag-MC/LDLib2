package com.lowdragmc.lowdraglib2.editor.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import java.io.File;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class FilePath implements IResourcePath {
    @Getter
    @EqualsAndHashCode.Include
    public final String path;
    @Getter
    public final File file;
    @Nullable
    @Getter
    public final ResourceLocation location;

    public static String normalizePath(String path) {
        if (path == null) return null;
        return path.replace('\\', '/')
                .replaceAll("/+", "/")
                .replaceAll("/$", "");
    }

    public FilePath(String path) {
        this.path = normalizePath(path);
        this.file = new File(this.path);
        this.location = toResourceLocation();
    }

    public FilePath(File file) {
        this.path = normalizePath(file.getPath());
        this.file = file;
        this.location = toResourceLocation();
    }

    public FilePath(ResourceLocation location) {
        this.path = "assets/" + location.getNamespace() + "/" + location.getPath();
        this.file = new File(this.path);
        this.location = location;
    }

    @Override
    public ResourceProviderType getType() {
        return FileResourceProvider.TYPE;
    }

    @Override
    public String getResourceName() {
        var name = file.getName();
        var dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1) ? name : name.substring(0, dotIndex);
    }

    @Nullable
    private ResourceLocation toResourceLocation() {
        var assetsIndex = path.indexOf("assets");
        if (assetsIndex == -1) return null;
        
        if (assetsIndex + 7 >= path.length() || path.charAt(assetsIndex + 6) != '/') {
            return null;
        }
        
        var remainPath = path.substring(assetsIndex + 7);
        var firstSlash = remainPath.indexOf('/');
        if (firstSlash == -1) {
            return null;
        }
        var namespace = remainPath.substring(0, firstSlash);
        var resourcePath = remainPath.substring(firstSlash + 1);
        
        if (namespace.isEmpty() || resourcePath.isEmpty()) {
            return null;
        }
        
        try {
            return ResourceLocation.fromNamespaceAndPath(namespace, resourcePath);
        } catch (Exception e) {
            return null;
        }
    }
}