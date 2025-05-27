package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

import com.lowdragmc.lowdraglib.editor.ui.view.ResourceView;
import net.minecraft.network.chat.Component;

public abstract class Resource<T> {
    @Getter
    protected final List<ResourceProvider<T>> providers = new ArrayList<>();

    public Resource() {
    }

    /**
     * Resource icon, it can be used to display the resource in the UI.
     */
    public abstract IGuiTexture getIcon();

    /**
     * Resource name, it can also be used to obtain the resource from the resource view. also see {@link ResourceView#getResourceByName(String)}
     */
    public abstract String getName();

    /**
     * The file extension for this resource type, used for {@link FileResourceProvider}
     */
    public String getFileExtension() {
        return "." + getName() + ".nbt";
    }

    /**
     * Generate default resources.
     */
    public void buildDefault() {
    }

    /**
     * Add a resource provider to this resource.
     */
    public void addResourceProvider(ResourceProvider<T> provider) {
        providers.add(provider);
    }

    /**
     * Remove a resource provider from this resource.
     */
    public void removeResourceProvider(ResourceProvider<T> provider) {
        providers.remove(provider);
    }

    public boolean displayPreviewName() {
        return true;
    }

    /**
     * Whether this resource can add a file resource provider. This is used to determine whether the button should be displayed in the UI.
     */
    public boolean canAddFileResourceProvider() {
        return true;
    }

    /**
     * Whether this resource can remove a resource provider. This is used to determine whether the remove button should be displayed in the UI.
     * By default, only FileResourceProvider can be removed.
     */
    public boolean canRemoveResourceProvider(ResourceProvider<T> provider) {
        return provider instanceof FileResourceProvider<T>;
    }

    /**
     * Create a new file resource provider for this resource. This is used to create a new resource provider that can read and write resources from files.
     */
    public FileResourceProvider<T> createNewFileResourceProvider(File directory) {
        return new FileResourceProvider<>(this, directory, getFileExtension());
    }

    /**
     * Create a resource provider container for the given provider. You should override it to attach additional UI elements or behaviors.
     * e.g. how to add a new resource, how to display the resource in the UI, etc.
     */
    public ResourceProviderContainer<T> createResourceProviderContainer(ResourceProvider<T> provider) {
        return provider.createContainer();
    }


    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    /**
     * Serialize resource to nbt for persistence.
     */
    @Nullable
    public abstract Tag serialize(T value, HolderLookup.Provider provider);

    /**
     * Deserialize resource from nbt.
     */
    @Nullable
    public abstract T deserialize(Tag nbt, HolderLookup.Provider provider);

    @Nullable
    public CompoundTag serializeNBT(T value, HolderLookup.Provider provider) {
        var tag = serialize(value, provider);
        if (tag == null) return null;
        var nbt = new CompoundTag();
        nbt.put("data", tag);
        nbt.putString("type", getName());
        return nbt;
    }

    @Nullable
    public T deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.getString("type").equals(getName())) {
            return deserialize(nbt.get("data"), provider);
        }
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

}
