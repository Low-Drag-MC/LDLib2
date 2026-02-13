package com.lowdragmc.lowdraglib2.gui.editor;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.*;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class UIEditor extends Editor {
    public final static ResourceLocation WINDOW_ID = LDLib2.id("ui_editor");


    public UIEditor() {
        this.leftWindow.setDisplay(false);
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
        fileMenu.addProjectProvider(UIXmlProjectType.TYPE);
    }

    @Override
    protected void closeCurrentProject() {
        super.closeCurrentProject();
        initResources();
    }
}
