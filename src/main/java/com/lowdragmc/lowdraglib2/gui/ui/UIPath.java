package com.lowdragmc.lowdraglib2.gui.ui;

import javax.annotation.Nullable;

public record UIPath(int[] path) {
    public UIPath of(UIElement element) {
        var structurePath = element.getStructurePath();
        var path = new int[structurePath.size() - 1];
        for (int i = 1; i < structurePath.size(); i++) {
            path[i - 1] = structurePath.get(i).getSiblingIndex();
        }
        return new UIPath(path);
    }

    @Nullable
    public UIElement drill(UIElement root) {
        for (int i : path) {
            if (i < 0 || i >= root.getChildren().size()) return null;
            root = root.getChildren().get(i);
        }
        return root;
    }
}
