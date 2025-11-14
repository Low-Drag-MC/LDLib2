package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;

public class TestEditor extends Editor {

    public TestEditor() {
    }

    @Override
    protected void initMenus() {
        super.initMenus();
        fileMenu.addProjectProvider(TestProject.TYPE);
    }
}
