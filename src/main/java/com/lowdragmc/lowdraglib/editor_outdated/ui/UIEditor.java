package com.lowdragmc.lowdraglib.editor_outdated.ui;

import java.io.File;

/**
 * @author KilaBash
 * @date 2023/6/3
 * @implNote UIEditor
 */
public class UIEditor extends Editor {

    public UIEditor(String modID) {
        super(modID);
    }

    public UIEditor(File workSpace) {
        super(workSpace);
    }

    @Override
    public String group() {
        return "editor";
    }

    @Override
    public String name() {
        return "editor.ui";
    }
}
