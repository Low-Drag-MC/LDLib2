package com.lowdragmc.lowdraglib2.gui.ui.elements;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
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

    @Test void pickerPathIsRelativeToTheWorkingDirectory() throws Exception {
        var assets = Files.createDirectories(directory.resolve("ldlib2/assets")).toFile();
        assertEquals(assets, FileDialogActions.typedTarget("./ldlib2/assets", true, directory.toFile(), assets));
    }

    @Test void saveDialogNameIsInsideTheOpenFolder() {
        var open = directory.resolve("sub").toFile();
        assertEquals(new File(open, "new.fxproj"), FileDialogActions.typedTarget("new.fxproj", false, directory.toFile(), open));
    }

    @Test void blankTargetIsTheOpenFolder() {
        assertEquals(directory.toFile(), FileDialogActions.typedTarget("  ", true, new File("elsewhere"), directory.toFile()));
    }

    @Test void onlyPathsUnderTheRootAreWithin() throws Exception {
        var root = Files.createDirectories(directory.resolve("root")).toFile();
        assertTrue(FileDialogActions.isWithin(root, root));
        assertTrue(FileDialogActions.isWithin(root, new File(root, "a/missing.txt")));
        assertFalse(FileDialogActions.isWithin(root, directory.toFile()));
        assertFalse(FileDialogActions.isWithin(root, new File(root, "../other")));
        assertFalse(FileDialogActions.isWithin(root, directory.resolve("rootx").toFile()));
    }

    @Test void unrepresentableNameIsJudgedBySpelling() {
        var root = directory.toFile();
        assertTrue(FileDialogActions.isWithin(root, new File(root, "bad\u0000name")));
    }

    @Test void invalidPathReturnsNoTarget() {
        assertNull(FileDialogActions.resolveTypedPath("invalid\u0000path", directory.toFile()));
        assertNull(FileDialogActions.navigationDirectory(null, false));
    }
}
