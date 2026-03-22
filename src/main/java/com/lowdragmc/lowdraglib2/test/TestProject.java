package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.resource.*;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraphResource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class TestProject implements IProject {
    public static final ProjectType TYPE = ProjectType.of(IGuiTexture.EMPTY, "project.test", ".test.nbt", TestProject::new);

    @Getter
    private final Resources resources;

    public TestProject() {
        this.resources = Resources.of(
                ColorsResource.INSTANCE,
                TexturesResource.INSTANCE,
                IRendererResource.INSTANCE,
                UIResource.INSTANCE,
                TestGraphResource.INSTANCE
        );
    }

    @Override
    public ProjectType getProjectType() {
        return TYPE;
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        return new CompoundTag();
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {

    }

}
