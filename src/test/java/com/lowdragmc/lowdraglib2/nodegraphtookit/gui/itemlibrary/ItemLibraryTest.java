package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemLibraryTest {

    @Test
    void sourceKeepsItemLibraryAsRootOverlay() throws IOException {
        var itemLibrarySource = Files.readString(Path.of(
                "src/main/java/com/lowdragmc/lowdraglib2/nodegraphtookit/gui/itemlibrary/ItemLibrary.java"));
        var graphViewSource = Files.readString(Path.of(
                "src/main/java/com/lowdragmc/lowdraglib2/nodegraphtookit/gui/GraphView.java"));

        assertTrue(itemLibrarySource.contains("var root = mui.ui.rootElement;"));
        assertTrue(itemLibrarySource.contains("root.worldToLocalLayoutOffset(new Vector2f(mouseX, mouseY))"));
        assertTrue(itemLibrarySource.contains("root.addChild(this);"));
        assertTrue(itemLibrarySource.contains("removeSelf();"));
        assertFalse(graphViewSource.contains("canvas.addChildren(graphView, panelLayer, itemLibrary)"));
    }
}
