package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileResourceProvider<T> extends ResourceProvider<T> {
    public final File resourceLocation;
    public final String resourceSuffix;
    protected final Map<File, Long> resourcesLastModified = new LinkedHashMap<>();

    public FileResourceProvider(Resource<T> resource, File resourceLocation, String resourceSuffix) {
        super(resource);
        this.resourceLocation = resourceLocation;
        this.resourceSuffix = resourceSuffix;
    }

    @Override
    public boolean supportResourcePath(IResourcePath path) {
        if (path instanceof FilePath filePath) {
            if (filePath.file().getName().endsWith(resourceSuffix)) {
                return filePath.file().getParentFile().equals(resourceLocation);
            }
        }
        return false;
    }

    @Override
    public IResourcePath createPath(String name) {
        return new FilePath(new File(resourceLocation, name + "." + resourceSuffix));
    }

    @Override
    public boolean addResource(IResourcePath path, T content) {
        if (supportResourcePath(path) && path instanceof FilePath filePath) {
            var file = filePath.file();
            try {
                var nbt = this.resource.serializeNBT(content, Platform.getFrozenRegistry());
                if (nbt != null) {
                    NbtIo.write(nbt, file.toPath());
                    resourcesLastModified.put(file, file.lastModified());
                    return super.addResource(path, content);
                } else {
                    LDLib.LOGGER.error("Failed to serialize resource {} to file {}", content, file);
                }
            } catch (IOException e) {
                LDLib.LOGGER.error("Failed to write resource {} to file {}", content, file, e);
            }
        }
        return false;
    }

    @Override
    public T removeResource(IResourcePath path) {
        if (supportResourcePath(path) && path instanceof FilePath filePath && filePath.file().isFile()) {
            if (filePath.file().delete()) {
                return super.removeResource(path);
            }
        }
        return null;
    }

    /**
     * Load and update resource
     * @return true resource changes.
     */
    public boolean tickResourceProvider() {
        if (resourceLocation == null) {
            return false;
        }
        var changed = false;
        var found = new HashSet<File>();
        var files = resourceLocation.listFiles((file, name) -> file.isFile() && name.endsWith(resourceSuffix));
        if (files != null) {
            for (var file : files) {
                var path = new FilePath(file);
                if (contents.containsKey(path)) {
                    if (!resourcesLastModified.containsKey(file) || resourcesLastModified.get(file) != file.lastModified()) {
                        var res = readResourceFromFile(file);
                        if (res != null) {
                            contents.put(path, res);
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
                        contents.put(path, resource);
                        resourcesLastModified.put(file, file.lastModified());
                        changed = true;
                        found.add(file);
                    }
                }
            }
        }
        if (found.size() != resourcesLastModified.size()) {
            var removed = new HashSet<>(resourcesLastModified.keySet());
            removed.removeAll(found);
            removed.forEach(file -> {
                resourcesLastModified.remove(file);
                contents.remove(new FilePath(file));
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
                var data = resource.deserializeNBT(fileData, Platform.getFrozenRegistry());
                if (data != null) return data;
            }
            LDLib.LOGGER.error("Failed to load resource file {} from {}: ", file, this);
        } catch (IOException e) {
            LDLib.LOGGER.error("Failed to load resource file {} from {}: ", file, this, e);
        }
        return null;
    }

}
