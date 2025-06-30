package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

import net.minecraft.network.chat.Component;

public abstract class Resource<T> {
    public enum DisplayMode {
        LIST,
        GRID,
    }
    @Getter
    protected final List<ResourceProvider<T>> providers = new ArrayList<>();
    @Getter @Setter
    private DisplayMode defaultDisplayMode = DisplayMode.GRID;
    @Getter @Setter
    private int defaultUIWidth = 30;

    public Resource() {
    }

    /**
     * Resource icon, it can be used to display the resource in the UI.
     */
    public abstract IGuiTexture getIcon();

    /**
     * Resource name, it can also be used to obtain the resource from the resource view.
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
    public void buildDefault(ResourceInstance<T> instance) {
        instance.addResourceProvider(createNewFileResourceProvider(new File(LDLib2.getAssetsDir(), "ldlib2/resources")).setName("global"));
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
     * Creates a new {@link FileResourceProvider} instance using the data from the given NBT tag.
     * @param tag The {@link CompoundTag} containing the serialized data for the file resource provider.
     * @return A {@link FileResourceProvider} created from the data in the given {@link CompoundTag}.
     */
    protected FileResourceProvider<T> createFileResourceProviderFromNBT(CompoundTag tag) {
        return FileResourceProvider.fromNBT(this, tag);
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
    public abstract Tag serializeResource(T value, HolderLookup.Provider provider);

    /**
     * Deserialize resource from nbt.
     */
    @Nullable
    public abstract T deserializeResource(Tag nbt, HolderLookup.Provider provider);

    @Override
    public String toString() {
        return getName();
    }

}
