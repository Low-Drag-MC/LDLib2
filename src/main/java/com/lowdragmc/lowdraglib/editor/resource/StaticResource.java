package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import lombok.Getter;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StaticResource<T> {
    public static StaticResource EMPTY = new StaticResource<>();

    private final Resource<T> resource;
    final Map<File, T> staticResources = new LinkedHashMap<>();
    final Map<File, Long> staticResourcesLastModified = new LinkedHashMap<>();
    // runtime
    @Getter
    private boolean isStaticResourceLoaded = false;

    public static <T> StaticResource<T> empty() {
        return EMPTY;
    }

    private StaticResource() {
        this.resource = null;
    }

    public StaticResource(Resource<T> resource) {
        this.resource = resource;
        if (!resource.supportStaticResource()) {
            throw new IllegalArgumentException("Resource " + resource + " does not support static resource");
        }
    }

    /**
     * Load and update static resource
     * @return true static resource changes.
     */
    public boolean loadAndUpdateStaticResource() {
        if (resource == null) {
            return false;
        }
        var changed = false;
        var found = new HashSet<File>();
        var files = resource.getStaticLocation().listFiles();
        if (files != null) {
            for (var file : files) {
                if (file.isFile() && file.getName().endsWith(resource.getFileResourceSuffix())) {
                    if (staticResources.containsKey(file)) {
                        if (staticResourcesLastModified.get(file) != file.lastModified()) {
                            var res = readResourceFromFile(file);
                            if (res != null) {
                                staticResources.put(file, res);
                                staticResourcesLastModified.put(file, file.lastModified());
                                changed = true;
                                found.add(file);
                            }
                        } else {
                            found.add(file);
                        }
                    } else {
                        var resource = readResourceFromFile(file);
                        if (resource != null) {
                            staticResources.put(file, resource);
                            staticResourcesLastModified.put(file, file.lastModified());
                            changed = true;
                            found.add(file);
                        }
                    }
                }
            }
        }
        if (found.size() != staticResources.size()) {
            var removed = new HashSet<>(staticResources.keySet());
            removed.removeAll(found);
            removed.forEach(file -> {
                staticResourcesLastModified.remove(file);
                staticResources.remove(file);
            });
            changed = true;
        }
        isStaticResourceLoaded = true;
        return changed;
    }

    @Nullable
    private T readResourceFromFile(File file) {
        try {
            var fileData = NbtIo.read(file.toPath());
            if (fileData != null && fileData.getString("type").equals(resource.name())) {
                return resource.deserialize(fileData.get("data"), Platform.getFrozenRegistry());
            } else {
                LDLib.LOGGER.error("Failed to load static resource file {} from {}: ", file, this);
            }
        } catch (IOException e) {
            LDLib.LOGGER.error("Failed to load static resource file {} from {}: ", file, this, e);
        }
        return null;
    }
}
