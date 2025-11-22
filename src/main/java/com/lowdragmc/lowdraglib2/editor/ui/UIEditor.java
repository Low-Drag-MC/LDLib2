package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.editor.resource.*;
import org.appliedenergistics.yoga.YogaDisplay;

public class UIEditor extends Editor {

    public UIEditor() {
        this.leftWindow.setDisplay(YogaDisplay.NONE);
        this.leftWindow.getParentWindow().removeSplitWindow(this.leftWindow);
        initResources();
    }

    private void initResources() {
        this.resourceView.clear();
        this.resourceView.loadResources(Resources.of(
                UIResource.INSTANCE,
                ColorsResource.INSTANCE,
                TexturesResource.INSTANCE
        ));
    }

    @Override
    protected void initMenus() {
        super.initMenus();
    }

    @Override
    protected void closeCurrentProject() {
        super.closeCurrentProject();
        initResources();
    }
}
