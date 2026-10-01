package com.lowdragmc.lowdraglib2.gui.ui.elements;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileDialogPathTest {
    @TempDir Path directory;

    @Test void resolvesRelativeAndQuotedAbsolutePaths() {
        assertEquals(directory.resolve("child").toFile(),
                FileDialogActions.resolveTypedPath("child", directory.toFile()));
        assertEquals(directory.toFile(), FileDialogActions.resolveTypedPath(
                "\"" + directory + "\"", directory.toFile()));
        assertEquals(directory.toFile(),
                FileDialogActions.resolveTypedPath("child/..", directory.toFile()));
    }

    @Test void blankInputUsesCurrentDirectory() {
        assertEquals(directory.toFile(), FileDialogActions.resolveTypedPath("  ", directory.toFile()));
    }

    @Test void fileInputNavigatesToItsParent() throws Exception {
        var file = Files.createFile(directory.resolve("effect.fxproj")).toFile();
        assertEquals(directory.toFile(), FileDialogActions.navigationDirectory(file, false));
        assertEquals(directory.toFile(), FileDialogActions.navigationDirectory(directory.toFile(), false));
    }

    @Test void missingFileIsAcceptedOnlyForSave() {
        var file = directory.resolve("new.fxproj").toFile();
        assertNull(FileDialogActions.navigationDirectory(file, false));
        assertEquals(directory.toFile(), FileDialogActions.navigationDirectory(file, true));
        assertNull(FileDialogActions.navigationDirectory(directory.resolve("missing/new.fxproj").toFile(), true));
    }

    @Test void invalidPathReturnsNoTarget() {
        assertNull(FileDialogActions.resolveTypedPath("invalid\u0000path", directory.toFile()));
        assertNull(FileDialogActions.navigationDirectory(null, false));
    }
}
