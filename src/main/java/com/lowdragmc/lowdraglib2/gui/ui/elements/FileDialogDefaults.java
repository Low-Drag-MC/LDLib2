package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.util.FileNode;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;

final class FileDialogDefaults {
    private FileDialogDefaults() {
    }

    static DefaultSelection resolve(FileNode root, boolean isSelector, @Nullable File defaultValue) {
        if (defaultValue == null) {
            return DefaultSelection.EMPTY;
        }
        if (isSelector) {
            var selectedNode = findVisibleFileNode(root, defaultValue);
            if (selectedNode == null) {
                return DefaultSelection.EMPTY;
            }
            return new DefaultSelection(selectedNode, selectedNode.getKey().toString());
        }
        if (defaultValue.isDirectory()) {
            return new DefaultSelection(findVisibleFileNode(root, defaultValue), "");
        }
        var parent = defaultValue.getParentFile();
        if (parent == null) {
            parent = root.getKey();
        }
        return new DefaultSelection(findVisibleFileNode(root, parent), defaultValue.getName());
    }

    static @Nullable FileNode findVisibleFileNode(FileNode root, @Nullable File file) {
        if (file == null) return null;
        var rootFile = normalizeFile(root.getKey());
        var targetFile = normalizeFile(file);
        var path = new ArrayDeque<File>();
        var currentFile = targetFile;
        while (currentFile != null) {
            path.addFirst(currentFile);
            if (rootFile.equals(currentFile)) {
                break;
            }
            currentFile = currentFile.getParentFile();
        }
        if (path.isEmpty() || !rootFile.equals(path.peekFirst())) {
            return null;
        }
        var currentNode = root;
        path.removeFirst();
        while (!path.isEmpty()) {
            var nextFile = path.removeFirst();
            FileNode nextNode = null;
            for (var child : currentNode.getChildren()) {
                if (normalizeFile(child.getKey()).equals(nextFile)) {
                    nextNode = child;
                    break;
                }
            }
            if (nextNode == null) {
                return null;
            }
            currentNode = nextNode;
        }
        return currentNode;
    }

    static File normalizeFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ignored) {
            return file.getAbsoluteFile();
        }
    }

    record DefaultSelection(@Nullable FileNode selectedNode, String text) {
        static final DefaultSelection EMPTY = new DefaultSelection(null, "");
    }
}
