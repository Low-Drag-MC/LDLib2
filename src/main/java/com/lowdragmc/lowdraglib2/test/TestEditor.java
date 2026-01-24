package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import org.appliedenergistics.yoga.YogaEdge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;


public class TestEditor extends Editor {
    public final CodeEditor stylesheetEditor = new CodeEditor();
    // runtime
    @Nullable
    private Stylesheet currentStyle;

    public TestEditor() {
        stylesheetEditor.setLanguage(Languages.LSS);
        stylesheetEditor.contentView.layout(layout -> layout.paddingAll(2));
        stylesheetEditor.contentView.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        stylesheetEditor.textAreaStyle(style -> style.focusOverlay(IGuiTexture.EMPTY));
        stylesheetEditor.layout(layout -> {
            layout.paddingAll(2);
            layout.heightPercent(100);
            layout.widthPercent(100);
        });
        stylesheetEditor.style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        stylesheetEditor.setLinesResponder(this::onStylesheetChanged);
        stylesheetEditor.getStyleBag().moveInlineAsDefault();
        var view = new View("Stylesheet Editor", Icons.LSS);
        view.addChildren(stylesheetEditor);
        centerWindow.getLeftTop().addView(view);
    }

    @Override
    protected @Nonnull Editor createNewEditorInstance() {
        return new TestEditor();
    }

    private void onStylesheetChanged(String[] lines) {
        var rawStyle = lines.length == 0 ? "" : String.join("\n", lines);
        var stylesheet = Stylesheet.parse(rawStyle);
        var mui = getModularUI();
        if (mui != null) {
            if (currentStyle != null) {
                mui.getStyleEngine().removeStylesheet(currentStyle);
            }
            currentStyle = stylesheet;
            mui.getStyleEngine().addStylesheet(currentStyle);
        }
    }

    @Override
    protected void initMenus() {
        super.initMenus();
        fileMenu.addProjectProvider(TestProject.TYPE);
    }
}
