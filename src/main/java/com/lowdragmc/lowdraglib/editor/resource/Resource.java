package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class Resource<T> {
    private final Map<Path, T> resources = new LinkedHashMap<>();
    private final Map<File, Long> resourcesLastModified = new LinkedHashMap<>();
    @Nullable
    @Getter
    private File resourceLocation;

    public Resource() {
    }

    /**
     * Resource name
     */
    public abstract String name();

    /**
     * Serialize resource to nbt for persistence.
     */
    @Nullable
    public abstract Tag serialize(T value, HolderLookup.Provider provider);

    /**
     * Deserialize resource from nbt.
     */
    public abstract T deserialize(Tag nbt, HolderLookup.Provider provider);

    @Nullable
    protected CompoundTag serializeNBT(T value, HolderLookup.Provider provider) {
        var tag = serialize(value, provider);
        if (tag == null) return null;
        var nbt = new CompoundTag();
        nbt.put("data", tag);
        nbt.putString("type", name());
        return nbt;
    }

    @Nullable
    public T deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.getString("type").equals(name())) {
            return deserialize(nbt.get("data"), provider);
        }
        return null;
    }

    public void setResourceLocation(@Nullable File resourceLocation) {
        this.resourceLocation = resourceLocation;
        this.resourcesLastModified.keySet().forEach(file -> resources.remove(file.toPath()));
        this.resourcesLastModified.clear();
    }

    /**
     * Generate default resources.
     */
    public void buildDefault() {
    }

    public void onLoad() {
    }

    public void unLoad() {
    }

    public boolean hasResource(Path key) {
        return resources.containsKey(key);
    }

    public void addResource(Path key, T resource) {
        var data = serializeNBT(resource, Platform.getFrozenRegistry());
        if (data == null) return;
        try {
            NbtIo.write(data, key);
            resources.put(key, resource);
        } catch (IOException e) {
            LDLib.LOGGER.error("Failed to save resource file {} from {}: ", key, this, e);
        }
    }

    /**
     * Add a resource file and save its file to the given location.
     */
    public void addResourceFile(File file, T resource) {
        addResource(file.toPath(), resource);
        if (resources.containsKey(file.toPath()) && file.isFile() && file.exists()) {
            resourcesLastModified.put(file, file.lastModified());
        }
    }

    public T removeResource(Path key) {
        if (resources.containsKey(key)) {
            resourcesLastModified.remove(key);
        }
        try {
            var file = key.toFile();
            if (file.isFile() && file.exists()) {
                try {
                    if (file.delete()) {
                        resources.remove(key);
                    } else {
                        LDLib.LOGGER.error("Failed to delete resource file {} from {}: ", key, this);
                    }
                } catch (Exception e) {
                    LDLib.LOGGER.error("Failed to delete resource file {} from {}: ", key, this, e);
                }
            }
        } catch (Exception e) {
            LDLib.LOGGER.error("Failed to delete resource file {} from {}: ", key, this, e);
        }
        return null;
    }

    public T getResource(Path key) {
        return resources.get(key);
    }

    public T getResourceOrDefault(Path key, T defaultValue) {
        return resources.getOrDefault(key, defaultValue);
    }

    public T getResourceOrSupply(Path key, Supplier<T> defaultValue) {
        return resources.containsKey(key) ? resources.get(key) : defaultValue.get();
    }

    public Stream<Map.Entry<Path, T>> allResources() {
        return resources.entrySet().stream();
    }

    public String getFileResourceSuffix() {
        return ".nbt";
    }

    /**
     * Load and update resource
     * @return true resource changes.
     */
    public boolean loadAndUpdateFileResource() {
        if (resourceLocation == null) {
            return false;
        }
        var changed = false;
        var found = new HashSet<File>();
        var files = resourceLocation.listFiles();
        if (files != null) {
            for (var file : files) {
                if (file.isFile() && file.getName().endsWith(getFileResourceSuffix())) {
                    var path = file.toPath();
                    if (resources.containsKey(path)) {
                        if (resourcesLastModified.get(file) != file.lastModified()) {
                            var res = readResourceFromFile(file);
                            if (res != null) {
                                resources.put(path, res);
                                resourcesLastModified.put(file, file.lastModified());
                                changed = true;
                                found.add(file);
                            }
                        } else {
                            found.add(file);
                        }
                    } else {
                        var resource = readResourceFromFile(file);
                        if (resource != null) {
                            resources.put(path, resource);
                            resourcesLastModified.put(file, file.lastModified());
                            changed = true;
                            found.add(file);
                        }
                    }
                }
            }
        }
        if (found.size() != resourcesLastModified.size()) {
            var removed = new HashSet<>(resourcesLastModified.keySet());
            removed.removeAll(found);
            removed.forEach(file -> {
                resourcesLastModified.remove(file);
                resources.remove(file.toPath());
            });
            changed = true;
        }
        return changed;
    }

    @Nullable
    private T readResourceFromFile(File file) {
        try {
            var fileData = NbtIo.read(file.toPath());
            if (fileData != null) {
                var data = deserializeNBT(fileData, Platform.getFrozenRegistry());
                if (data != null) return data;
            }
            LDLib.LOGGER.error("Failed to load resource file {} from {}: ", file, this);
        } catch (IOException e) {
            LDLib.LOGGER.error("Failed to load resource file {} from {}: ", file, this, e);
        }
        return null;
    }

    @Override
    public String toString() {
        return name();
    }

}
