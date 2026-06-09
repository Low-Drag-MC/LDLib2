package com.lowdragmc.lowdraglib2.editor.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ProjectTypeDefaultSaveFileTest {
    @TempDir
    Path tempDir;

    @Test
    void baseProjectTypeUsesNewFileWithSuffix() {
        var project = mock(IProject.class);
        var projectType = new ProjectType(null, "test", ".test", () -> project);

        assertEquals(tempDir.resolve("new.test").toFile(), projectType.getDefaultSaveFile(project, tempDir.toFile()));
    }

    @Test
    void baseProjectTypeAddsNumberWhenDefaultFileExists() throws IOException {
        Files.createFile(tempDir.resolve("new.test"));
        Files.createFile(tempDir.resolve("new_1.test"));
        var project = mock(IProject.class);
        var projectType = new ProjectType(null, "test", ".test", () -> project);

        assertEquals(tempDir.resolve("new_2.test").toFile(), projectType.getDefaultSaveFile(project, tempDir.toFile()));
    }

    @Test
    void projectTypeCanSpecifyDefaultSaveFile() {
        var project = mock(IProject.class);
        var projectType = new ProjectType(null, "test", ".test", () -> project) {
            @Override
            public File getDefaultSaveFile(IProject project, File projectRoot) {
                return new File(getRootSavePath(project, projectRoot), "custom.test");
            }
        };
        var root = new File("root");

        assertEquals(new File(root, "custom.test"), projectType.getDefaultSaveFile(project, root));
    }
}
