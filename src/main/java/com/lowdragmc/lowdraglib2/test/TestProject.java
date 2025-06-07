package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor.resource.IRendererResource;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.editor.ui.menu.FileMenu;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class TestProject implements IProject {
    public static final FileMenu.ProjectProvider PROVIDER = FileMenu.ProjectProvider.of(IGuiTexture.EMPTY, "project.test", ".test.nbt", TestProject::new);

    @Getter
    private final Resources resources;

    public TestProject() {
        this.resources = Resources.of(
          new ColorsResource(),
          new TexturesResource(),
          new IRendererResource()
        );
    }

    @Override
    public String getSuffix() {
        return PROVIDER.getSuffix();
    }

    @Override
    public String getName() {
        return PROVIDER.getName();
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        return new CompoundTag();
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {

    }

}
