package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.resource.*;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import lombok.Getter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
                UIResource.INSTANCE
        );
    }

    @Override
    public ProjectType getProjectType() {
        return TYPE;
    }

    @Override
    public void serializeProject(@NotNull ValueOutput output) {

    }

    @Override
    public void deserializeProject(@NotNull ValueInput input) {

    }
}
