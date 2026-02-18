package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import net.minecraft.nbt.CompoundTag;

import org.jetbrains.annotations.Nullable;

public abstract class ResourceProviderType {

    /**
     * Retrieves the type name of the resource provider.
     *
     * @return the type name in {@code String} format representing the resource provider type.
     */
    public abstract String getTypeName();

    public abstract IGuiTexture getIcon();

    /**
     * Creates a full resource path based on the specified partial path.
     * The generated {@code IResourcePath} instance can represent either a built-in
     * or file-backed resource path, depending on the implementation.
     *
     * @param path a {@code String} representing the initial partial path
     *             that will be used to construct the full resource path.
     * @return an {@link IResourcePath} instance representing the constructed full path
     *         with type and path details corresponding to the resource provider implementation.
     */
    public abstract IResourcePath createFullPath(String path);

    /**
     * Determines whether custom resources are supported by the resource provider.
     *
     * @return {@code true} if custom resources are supported, otherwise {@code false}.
     */
    public boolean supportCustom() {
        return false;
    }

    /**
     * Deserializes a {@link ResourceProvider} instance of the specified resource type from the given NBT data.
     * This method interprets the data stored in the {@code CompoundTag} and reconstructs a corresponding
     * {@link ResourceProvider} instance if possible. If deserialization fails or the data is invalid, it will return {@code null}.
     *
     * @param resourceHolder the {@link ResourceInstance} representing the type and details of the resource
     *                       associated with the {@link ResourceProvider}.
     * @param tag a {@code CompoundTag} containing the serialized data for reconstructing the {@link ResourceProvider}.
     * @param <T> the type of resource managed by the {@link ResourceProvider}.
     * @return a {@link ResourceProvider} instance reconstructed from the NBT data, or {@code null} if the data is invalid
     *         or the deserialization process fails.
     */
    @Nullable
    public <T> ResourceProvider<T> fromNbt(ResourceInstance<T> resourceHolder, CompoundTag tag) {
        return null;
    }

    /**
     * Called when a custom resource is being created within the specified {@code ResourceContainer}.
     * This method allows for additional customization or initialization of elements specific to the resource.
     *
     * @param container the {@link ResourceContainer} that holds data and UI elements related to the resource.
     *                  This container encapsulates the resource instance, editor, and toggles for resource providers.
     * @param <T>       the type of the resource managed within the {@code ResourceContainer}.
     */
    public <T> void onCreateCustom(ResourceContainer<T> container) {

    }

}
