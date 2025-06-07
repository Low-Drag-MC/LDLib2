package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaOverflow;

import java.util.Map;
import java.util.function.Supplier;

public interface IResourceProvider<T> extends Iterable<Map.Entry<IResourcePath, T>> {

    String getName();

    IGuiTexture getIcon();

    Resource<T> getResourceHolder();

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
     * Get the name of the resource from the resource path.
v     */
    default String getResourceName(IResourcePath path) {
        return path.getResourceName();
    }

    /**
     * Create a container ui for this resource provider.
     * This is used to display the resources in the UI.
     * @return a new ResourceProviderContainer for this provider.
     */
    default ResourceProviderContainer<T> createContainer() {
        return new ResourceProviderContainer<>(this);
    }

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

    default boolean supportAdd() {
        return true;
    }

    /**
     * Create a toggle UI element to switch resource provider.
     */
    default UIElement createProviderToggle() {
        return new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 2);
        }).addChildren(
                new UIElement().layout(layout -> {
                    layout.setWidth(9);
                    layout.setHeight(9);
                }).style(style -> style.backgroundTexture(getIcon())),
                new Label().textStyle(textStyle -> textStyle.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL))
                        .setText(getName())
                        .layout(layout -> layout.setFlex(1))
                        .setOverflow(YogaOverflow.HIDDEN)
        );
    }

    /**
     * Called when the open a menu for the resource provider.
     * @param menu
     */
    default void onMenu(TreeBuilder.Menu menu) {

    }

}
