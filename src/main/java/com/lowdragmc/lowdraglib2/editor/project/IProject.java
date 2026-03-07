package com.lowdragmc.lowdraglib2.editor.project;

import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jetbrains.annotations.NotNull;


/**
 * Interface for a project in the editor.
 */
public interface IProject extends ValueIOSerializable {
    /**
     * Get Resources of this project
     */
    Resources getResources();

    /**
     * Get the type of this project.
     */
    ProjectType getProjectType();

    /**
     * Get the file suffix for this project. It will be used to save the project file or load it.
     */
    default String getSuffix() {
        return getProjectType().getSuffix();
    }

    /**
     * Get the name of this project.
     */
    default String getName() {
        return getProjectType().getName();
    }

    /**
     * Serialize the project to NBT.
     * This method will be called when saving the project.
     */
    void serializeProject(@NotNull ValueOutput output);

    /**
     * Deserialize the project from NBT.
     */
    void deserializeProject(@NotNull ValueInput input);

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
    default void serializeMetadata(ValueOutput output) {
        output.putString("version", getVersion());
        output.putString("suffix", getSuffix());
        output.putString("name", getName());
    }

    /**
     * Deserialize metadata of this project.
     */
    default void deserializeMetadata(ValueInput input) {

    }

    @Override
    default void serialize(@NotNull ValueOutput valueOutput) {
        serializeMetadata(valueOutput.child("meta"));
        serializeProject(valueOutput.child("data"));
    }

    @Override
    default void deserialize(@NotNull ValueInput valueInput) {
        valueInput.child("meta").ifPresent(this::deserializeMetadata);
        valueInput.child("data").ifPresent(this::deserializeProject);
    }
}
