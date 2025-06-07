package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.editor_outdated.Icons;

public class BuiltinResourceProvider<T> extends ResourceProvider<T>{
    public BuiltinResourceProvider(Resource<T> resourceHolder) {
        super(resourceHolder);
        setName("editor.builtin");
        setIcon(Icons.RESOURCE);
    }

    @Override
    public boolean supportResourcePath(IResourcePath path) {
        return path instanceof BuiltinPath;
    }

    @Override
    public IResourcePath createPath(String name) {
        return new BuiltinPath(name);
    }

    @Override
    public boolean canRemove(IResourcePath path) {
        return false;
    }

    @Override
    public boolean canRename(IResourcePath path) {
        return false;
    }

    @Override
    public boolean canEdit(IResourcePath path) {
        return false;
    }

    @Override
    public boolean canCopy(IResourcePath path) {
        return false;
    }

    @Override
    public boolean supportAdd() {
        return false;
    }
}
