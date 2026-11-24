package com.lowdragmc.lowdraglib2.editor.resource;

import lombok.Getter;
import net.minecraft.network.chat.Component;

/**
 * Resource types the resource view shows behind a single tab, the way an image editor keeps related tools behind
 * one toolbar button: the tab wears the icon of the one last picked, and clicking it opens a panel listing them all
 * by icon and name.
 * <p>
 * Which resources a group holds is the project's to say ({@link Resources.Builder#group}), not the resources': the same
 * type can be grouped differently in different editors. A group is its id, so two instances with the same id are the same
 * group, and the first one the view meets supplies the name.
 */
@Getter
public final class ResourceGroup {
    private final String id;
    private final Component displayName;

    public ResourceGroup(String id, Component displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public static ResourceGroup of(String id, String translationKey) {
        return new ResourceGroup(id, Component.translatable(translationKey));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ResourceGroup group && group.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ResourceGroup[" + id + "]";
    }
}
