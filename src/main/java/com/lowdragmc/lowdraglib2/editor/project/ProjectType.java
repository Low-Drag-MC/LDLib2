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

    /**
     * Retrieves the root save path for the given project.
     *
     * This method determines and returns the root directory where the project data
     * will be saved or accessed during operations.
     *
     * @param project The {@link IProject} instance for which the root save path is to be determined.
     *                This project should provide relevant information for path resolution.
     * @param projectRoot The {@link File} instance representing the root directory of the project.
     *                    Must be a valid directory and accessible.
     * @return The {@link File} instance representing the resolved root save path for the given project.
     */
    public File getRootSavePath(IProject project, File projectRoot) {
        return projectRoot;
    }

    /**
     * Retrieves the default file to prefill when saving the given project.
     *
     * @param project The {@link IProject} instance for which the default save file is to be determined.
     * @param projectRoot The {@link File} instance representing the root directory of the project.
     * @return The default save file.
     */
    public File getDefaultSaveFile(IProject project, File projectRoot) {
        var savePath = getRootSavePath(project, projectRoot);
        var defaultFile = new File(savePath, "new" + suffix);
        var index = 1;
        while (defaultFile.exists()) {
            defaultFile = new File(savePath, "new_" + index++ + suffix);
        }
        return defaultFile;
    }

    /**
     * Loads a project from the specified file.
     * The method reads serialized project data from the provided file, creates an instance of the project using the
     * {@link #getProjectCreator()} supplier, and deserializes the data into the project object.
     *
     * @param file The file from which the project data should be loaded.
     *             The file must contain valid serialized project data in the proper format.
     * @return An {@link IProject} instance representing the loaded project.
     * @throws Exception If any error occurs during file reading, data deserialization, or object creation.
     */
    public IProject loadProjectFromFile(File file) throws Exception {
        var data = NbtIo.read(file.toPath());
        var project = getProjectCreator().get();
        project.deserializeNBT(Platform.getFrozenRegistry(), Objects.requireNonNull(data));
        return project;
    }

    /**
     * Saves a project to a specified file. This method serializes the given project into an NBT format
     * using the platform's frozen registry and writes the serialized data to the specified file.
     *
     * @param project The {@link IProject} instance to save. The project must be properly initialized
     *                and its data serializable.
     * @param file    The {@link File} where the serialized project data will be saved. The file must
     *                be writable, and its location accessible.
     * @throws Exception If an error occurs during project serialization or file writing.
     */
    public void saveProjectToFile(IProject project, File file) throws Exception {
        var fileData = project.serializeNBT(Platform.getFrozenRegistry());
        NbtIo.write(fileData, file.toPath());
    }

    /**
     * Checks if a project is "dirty" compared to its serialized file representation. A project is considered "dirty"
     * if its serialized data differs from the data stored in the file.
     *
     * @param project The {@link IProject} instance to verify. This project must be initialized and provide serialization
     *                capabilities via {@code serializeNBT}.
     * @param file    The {@link File} to compare against. The file must exist and contain valid serialized project data.
     * @return {@code true} if the project's serialized data is different from the file's serialized data,
     *         {@code false} otherwise.
     * @throws Exception If an error occurs while serializing the project or reading the file.
     */
    public boolean isProjectDirty(IProject project, File file) throws Exception {
        var data = project.serializeNBT(Platform.getFrozenRegistry());
        var fileData = NbtIo.read(file.toPath());
        return !data.equals(fileData);
    }

    /**
     * Creates a new empty project.
     * This method utilizes the {@code projectCreator} supplier to instantiate an {@link IProject}
     * and initializes it using the {@link IProject#initNewProject()} method.
     *
     * @return The newly created empty {@link IProject} instance.
     */
    public IProject newEmptyProject() {
        var project = projectCreator.get();
        project.initNewProject();
        return project;
    }
}
