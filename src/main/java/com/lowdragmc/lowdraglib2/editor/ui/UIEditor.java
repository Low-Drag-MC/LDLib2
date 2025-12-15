package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.*;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaDisplay;

import javax.annotation.Nonnull;

public class UIEditor extends Editor {
    public final static ResourceLocation WINDOW_ID = LDLib2.id("ui_editor");

    public UIEditor() {
        this.leftWindow.setDisplay(YogaDisplay.NONE);
        this.leftWindow.getParentWindow().removeSplitWindow(this.leftWindow);
        initResources();
    }

    @Override
    protected @Nonnull Editor createNewEditorInstance() {
        return new UIEditor();
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
