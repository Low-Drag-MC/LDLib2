package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.nbt.CompoundTag;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaOverflow;

import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.function.Supplier;

public interface IResourceProvider<T> extends Iterable<Map.Entry<IResourcePath, T>> {

    String getName();

    ResourceInstance<T> getResourceInstance();

    boolean hasResource(IResourcePath key);

    /**
     * Creates a subpath within the resource hierarchy using the specified name.
     *
     * @param name The name of the subpath to be created. Must not be {@code null}.
     * @return The {@link IResourcePath} representing the newly created subpath.
     */
    IResourcePath createSubPath(String name);

    /**
     * Retrieves the type of the resource provider.
     *
     * @return the {@link ResourceProviderType} associated with this resource provider.
     */
    ResourceProviderType getType();

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
    @Nullable T removeResource(IResourcePath path);

    /**
     * Get a resource from the provider.
     * @param path The resource path to get.
     * @return the resource, or null if the resource was not found.
     */
    @Nullable T getResource(IResourcePath path);

    default @Nullable CompoundTag serializeNBT() {
        return null;
    }

    /**
     * Get the name of the resource from the resource path.
v     */
    default String getResourceName(IResourcePath path) {
        return path.getResourceName();
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
    default boolean checkAndUpdateResourceProvider() {
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
            layout.widthPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(2);
        }).addChildren(
                new UIElement().layout(layout -> {
                    layout.width(9);
                    layout.height(9);
                }).style(style -> style.backgroundTexture(getType().getIcon())),
                new Label().textStyle(textStyle -> textStyle.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL))
                        .setText(getName())
                        .layout(layout -> layout.flex(1))
                        .setOverflowVisible(false)
        );
    }

    /**
     * Called when the open a menu for the resource provider.
     * @param menu
     */
    default void onMenu(TreeBuilder.Menu menu) {

    }
}
