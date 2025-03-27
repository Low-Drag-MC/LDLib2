package com.lowdragmc.lowdraglib.editor.ui;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;

import java.io.File;

/**
 * @author KilaBash
 * @date 2023/6/3
 * @implNote UIEditor
 */
@LDLRegister(name = "editor.ui", group = "editor")
public class UIEditor extends Editor {
    public UIEditor(File workSpace) {
        super(workSpace);
    }

    public UIEditor(String modID) {
        super(modID);
    }
}
