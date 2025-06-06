package com.lowdragmc.lowdraglib.test;

import com.lowdragmc.lowdraglib.editor.ui.Editor;
import com.lowdragmc.lowdraglib.editor.ui.view.ui.UIEditorView;
import com.lowdragmc.lowdraglib.gui.ui.UI;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import org.appliedenergistics.yoga.YogaEdge;

public class TestEditor extends Editor {

    @Override
    protected void initMenus() {
        super.initMenus();
        fileMenu.addProjectProvider(TestProject.PROVIDER);
    }

    @Override
    protected void initCenterWindow() {
        super.initCenterWindow();
        center.addView(new UIEditorView(UI.of(new UIElement().layout(layout -> {
            layout.setWidth(250);
            layout.setHeight(250);
            layout.setPadding(YogaEdge.ALL, 10);
        }).addChildren(new Button(), new Button(), new Label()).style(style -> style.backgroundTexture(Sprites.BORDER)))));
    }
}
