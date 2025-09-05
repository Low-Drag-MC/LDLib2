package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import lombok.Getter;


public class BuiltinResourceProvider<T> extends ResourceProvider<T> {
    public static final ResourceProviderType TYPE = new ResourceProviderType() {
        @Override
        public String getTypeName() {
            return "built-in";
        }

        @Override
        public IGuiTexture getIcon() {
            return Icons.RESOURCE;
        }

        @Override
        public IResourcePath createFullPath(String path) {
            if (!path.contains(":")) {
                path = "built-in:" + path;
            }
            return new BuiltinPath(path);
        }
    };

    @Getter
    public final String name;

    public BuiltinResourceProvider(String name, ResourceInstance<T> resourceInstance) {
        super(resourceInstance);
        this.name = name;
    }

    @Override
    public boolean supportResourcePath(IResourcePath path) {
        return path instanceof BuiltinPath;
    }

    @Override
    public ResourceProviderType getType() {
        return TYPE;
    }

    @Override
    public IResourcePath createSubPath(String name) {
        return new BuiltinPath("%s:%s".formatted(this.name, name));
    }

    @Override
    public String getResourceName(IResourcePath path) {
        if (path instanceof BuiltinPath builtinPath) {
            if (builtinPath.getPath().startsWith(name + ":")) {
                return builtinPath.getPath().substring(name.length() + 1);
            }
        }
        return super.getResourceName(path);
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
