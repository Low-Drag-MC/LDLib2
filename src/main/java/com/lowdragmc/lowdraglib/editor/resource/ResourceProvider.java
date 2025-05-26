package com.lowdragmc.lowdraglib.editor.resource;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ResourceProvider<T> implements IResourceProvider<T> {
    public final Resource<T> resource;
    @Getter
    protected final Map<IResourcePath, T> contents = new LinkedHashMap<>();
    
    protected ResourceProvider(Resource<T> resource) {
        this.resource = resource;
    }

    public abstract boolean supportResourcePath(IResourcePath path);

    public boolean hasResource(IResourcePath path) {
        if (!supportResourcePath(path)) return false;
        return contents.containsKey(path);
    }

    @Override
    public T getResource(IResourcePath path) {
        if (supportResourcePath(path) && contents.containsKey(path)) {
            return contents.get(path);
        }
        return null;
    }

    public boolean addResource(IResourcePath path, T resource) {
        if (!supportResourcePath(path)) return false;
        contents.put(path, resource);
        return true;
    }

    public T removeResource(IResourcePath path) {
        if (supportResourcePath(path) && contents.containsKey(path)) {
            return contents.remove(path);
        }
        return null;
    }

    @Override
    public boolean canRemove(IResourcePath path) {
        return supportResourcePath(path);
    }

    @Override
    public boolean canRename(IResourcePath path) {
        return supportResourcePath(path);
    }

    @Override
    public boolean canEdit(IResourcePath path) {
        return supportResourcePath(path);
    }

    @Override
    public boolean canCopy(IResourcePath path) {
        return supportResourcePath(path);
    }

    @Override
    public @NotNull Iterator<Map.Entry<IResourcePath, T>> iterator() {
        return contents.entrySet().iterator();
    }
}
