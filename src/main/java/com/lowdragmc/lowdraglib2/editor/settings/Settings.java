package com.lowdragmc.lowdraglib2.editor.settings;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public interface Settings extends IConfigurable {
    /**
     * Unique ID for this settings registry.
     */
    ResourceLocation getId();

    /**
     * Retrieves the path associated with the current settings.
     *
     * @return the path as a {@code String}, typically representing a structured hierarchy
     *         or logical categorization for these settings. The path may use delimiters
     *         like periods (e.g., {@code "parent.child"}).
     */
    String getPath();

    /**
     * Applies the specified action or changes using the provided {@link Editor}.
     * This method is intended to customize or configure settings in line with
     * the provided {@code Editor}'s state or directives.
     *
     * @param editor the {@link Editor} instance used to apply modifications
     *               or retrieve information for this configuration.
     */
    void onApply(Editor editor);

    /**
     * Retrieves the display name for this settings registry.
     */
    default Component getDisplayName() {
        return Component.translatable(getId().toLanguageKey("settings"));
    }
}
