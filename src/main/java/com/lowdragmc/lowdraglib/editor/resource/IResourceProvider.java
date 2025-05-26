package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceProviderContainer;

import java.util.Map;
import java.util.function.Supplier;

public interface IResourceProvider<T> extends Iterable<Map.Entry<IResourcePath, T>> {

    boolean hasResource(IResourcePath key);

    IResourcePath createPath(String name);

    /**
     * Add a resource to the provider. if the resource is existing, it will be replaced.
     * @param path The resource path.
     * @param resource The resource to add.
     * @return true if the resource was added successfully, false if it cannot be added (e.g. if the resource is null or the path is invalid).
     */
    boolean addResource(IResourcePath path, T resource);

    /**
     * Remove a resource from the provider.
     * @param path The resource path to remove.
     * @return the removed resource, or null if the resource was not found.
     */
    T removeResource(IResourcePath path);

    /**
     * Get a resource from the provider.
     * @param path The resource path to get.
     * @return the resource, or null if the resource was not found.
     */
    T getResource(IResourcePath path);

    /**
     * Create a container ui for this resource provider.
     * This is used to display the resources in the UI.
     * @return a new ResourceProviderContainer for this provider.
     */
    ResourceProviderContainer<T> createContainer();

    default T getResourceOrDefault(IResourcePath path, T defaultValue) {
        var resource = getResource(path);
        return resource != null ? resource : defaultValue;
    }

    default T getResourceOrSupply(IResourcePath path, Supplier<T> defaultValue) {
        var resource = getResource(path);
        return resource != null ? resource : defaultValue.get();
    }

    /**
     * called every tick to update the resource provider.
     * This can be used to reload resources, check for changes, etc.
     * @return true if the resource provider has changed, false otherwise.
     */
    default boolean tickResourceProvider() {
        return false;
    }

    default boolean canRemove(IResourcePath path) {
        return true;
    }

    default boolean canRename(IResourcePath path) {
        return true;
    }

    default boolean canEdit(IResourcePath path) {
        return true;
    }

    default boolean canCopy(IResourcePath path) {
        return true;
    }

}
