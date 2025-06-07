package com.lowdragmc.lowdraglib2.editor.project;

import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;

/**
 * Interface for a project in the editor.
 */
public interface IProject extends INBTSerializable<CompoundTag> {
    /**
     * Get Resources of this project
     */
    Resources getResources();

    /**
     * Get the file suffix for this project. It will be used to save the project file or load it.
     */
    String getSuffix();

    /**
     * Get the name of this project.
     */
    String getName();

    /**
     * Serialize the project to NBT.
     * This method will be called when saving the project.
     */
    CompoundTag serializeProject(@Nonnull HolderLookup.Provider provider);

    /**
     * Deserialize the project from NBT.
     */
    void deserializeProject(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag nbt);

    /**
     * Get the display name of this project.
     */
    default Component getDisplayName() {
        return Component.translatable(getName());
    }

    /**
     * Initialize a new empty project. This method will be called when creating a new project and before {@link #onLoad(Editor)}.
     */
    default void initNewProject() {
        getResources().buildDefault();
    }

    /**
     * Fired when the project is closed
     */
    default void onClosed(Editor editor) {
    }

    /**
     * Fired when the project is opened
     */
    default void onLoad(Editor editor) {
    }

    /**
     * Get the version of this project.
     * Default version, can be overridden by specific projects.
     * It will be stored in the project file.
     */
    default String getVersion() {
        return "1.0";
    }

    /**
     * Get metadata of this project. e.g. version, suffix, name.
     */
    default CompoundTag getMetadata() {
        var data = new CompoundTag();
        data.putString("version", getVersion());
        data.putString("suffix", getSuffix());
        data.putString("name", getName());
        return data;
    }

    @Override
    default CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var data = new CompoundTag();
        data.put("meta", getMetadata());
        data.put("resources", getResources().serializeNBT(provider));
        data.put("data", serializeProject(provider));
        return data;
    }

    @Override
    default void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag nbt) {
        getResources().deserializeNBT(provider, nbt.getCompound("resources"));
        deserializeProject(provider, nbt.getCompound("data"));
    }
}
