package com.lowdragmc.lowdraglib2.editor.project;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.util.Objects;
import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public class ProjectType {
    public final IGuiTexture icon;
    public final String name;
    public final String suffix;
    public final Supplier<IProject> projectCreator;

    public static ProjectType of(IGuiTexture icon, String name, String suffix, Supplier<IProject> projectCreator) {
        return new ProjectType(icon, name, suffix, projectCreator);
    }

    public IProject loadProjectFromFile(File file) throws Exception {
        var data = NbtIo.read(file.toPath());
        var project = getProjectCreator().get();
        project.deserializeNBT(Platform.getFrozenRegistry(), Objects.requireNonNull(data));
        return project;
    }

    public void saveProjectToFile(IProject project, File file) throws Exception {
        var fileData = project.serializeNBT(Platform.getFrozenRegistry());
        NbtIo.write(fileData, file.toPath());
    }

    public boolean isProjectDirty(IProject project, File file) throws Exception {
        var data = project.serializeNBT(Platform.getFrozenRegistry());
        var fileData = NbtIo.read(file.toPath());
        return !data.equals(fileData);
    }
}
