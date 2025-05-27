package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Accessors(chain = true)
public abstract class ResourceProvider<T> implements IResourceProvider<T> {
    @Getter
    public final Resource<T> resourceHolder;
    @Getter
    protected final Map<IResourcePath, T> contents = new LinkedHashMap<>();
    @Getter @Setter
    private String name;
    @Getter @Setter
    private IGuiTexture icon;
    
    protected ResourceProvider(Resource<T> resourceHolder) {
        this.resourceHolder = resourceHolder;
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

    public boolean addResource(String name, T resource) {
        return addResource(createPath(name), resource);
    }

    public T removeResource(IResourcePath path) {
        if (supportResourcePath(path) && contents.containsKey(path)) {
            return contents.remove(path);
        }
        return null;
    }

    public T removeResource(String name) {
        return removeResource(createPath(name));
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
