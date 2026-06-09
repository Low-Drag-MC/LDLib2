package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.util.FileNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DialogFileDefaultTest {
    @TempDir
    Path tempDir;

    @Test
    void selectorDefaultResolvesVisibleFileAndPrefillsPath() throws IOException {
        var folder = Files.createDirectories(tempDir.resolve("folder"));
        var file = Files.writeString(folder.resolve("image.png"), "content").toFile();
        var root = new FileNode(tempDir.toFile());

        var selection = FileDialogDefaults.resolve(root, true, file);

        var selected = selection.selectedNode();
        assertNotNull(selected);
        assertEquals(file.getCanonicalFile(), selected.getKey().getCanonicalFile());
        assertEquals(file.toString(), selection.text());
    }

    @Test
    void selectorDefaultFilteredOutDoesNotSelectOrPrefill() throws IOException {
        var file = Files.writeString(tempDir.resolve("filtered.tmp"), "content").toFile();
        var root = new FileNode(tempDir.toFile()).setValid(node -> !node.getKey().getName().endsWith(".tmp"));

        var selection = FileDialogDefaults.resolve(root, true, file);

        assertNull(selection.selectedNode());
        assertEquals("", selection.text());
    }

    @Test
    void createDefaultFileSelectsParentDirectoryAndPrefillsFileName() throws IOException {
        var folder = Files.createDirectories(tempDir.resolve("folder")).toFile();
        var defaultFile = new File(folder, "new-file.json");
        var root = new FileNode(tempDir.toFile());

        var selection = FileDialogDefaults.resolve(root, false, defaultFile);

        var selected = selection.selectedNode();
        assertNotNull(selected);
        assertEquals(folder.getCanonicalFile(), selected.getKey().getCanonicalFile());
        assertEquals("new-file.json", selection.text());
    }

    @Test
    void createDefaultDirectorySelectsDirectoryAndLeavesFileNameEmpty() throws IOException {
        var folder = Files.createDirectories(tempDir.resolve("folder")).toFile();
        var root = new FileNode(tempDir.toFile());

        var selection = FileDialogDefaults.resolve(root, false, folder);

        var selected = selection.selectedNode();
        assertNotNull(selected);
        assertEquals(folder.getCanonicalFile(), selected.getKey().getCanonicalFile());
        assertEquals("", selection.text());
    }
}
