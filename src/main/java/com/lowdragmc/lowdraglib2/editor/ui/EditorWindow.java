package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import lombok.Getter;
import org.appliedenergistics.yoga.*;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class EditorWindow extends UIElement {
    public final Supplier<Editor> editorCreator;
    public final UIElement editorButtonContainer = new UIElement();

    @Getter
    @Nullable
    private Editor currentEditor;
    @Getter
    private final LinkedHashMap<Editor, UIElement> editors = new LinkedHashMap<>();

    public EditorWindow(Supplier<Editor> editorCreator) {
        getLayout().setWidthPercent(100);
        getLayout().setHeightPercent(100);

        this.editorCreator = editorCreator;
        this.editorButtonContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setPosition(YogaEdge.TOP, 15);
            layout.setWidthPercent(100);
            layout.setGap(YogaGutter.ALL, 1);
            layout.setHeight(14);
        }).setDisplay(YogaDisplay.NONE).style(style -> style.backgroundTexture(ColorPattern.BLACK.rectTexture()));
        this.editorButtonContainer.addClass("__editor-window_editor-button-container__").moveInlineAsDefault();

        addChild(editorButtonContainer);
        createNewEditor();
    }

    public boolean hasMultipleEditors() {
        return editors.size() > 1;
    }

    public void showEditor(Editor editor) {
        if (currentEditor == editor) return;
        if (currentEditor != null) {
            currentEditor.setDisplay(YogaDisplay.NONE);
        }
        currentEditor = editor;
        editor.setDisplay(YogaDisplay.FLEX);
        editor.mainView.layout(layout -> {
            layout.setMargin(YogaEdge.TOP, hasMultipleEditors() ? 14 : 0);
        });
        editorButtonContainer.setDisplay(hasMultipleEditors() ? YogaDisplay.FLEX : YogaDisplay.NONE);
        for (var entry : editors.entrySet()) {
            var isCurrent = entry.getKey() == currentEditor;
            entry.getValue().style(style -> {
                style.setPipelineState(StyleOrigin.DEFAULT);
                style.backgroundTexture(isCurrent ? ColorPattern.SLATE_PLUM.rectTexture() : ColorPattern.DARK_GRAY.rectTexture());
                style.setPipelineState(StyleOrigin.INLINE);
            }).addClass(isCurrent ? "__editor-window_active__" : "__editor-window_inactive__")
                    .removeClass(isCurrent ? "__editor-window_inactive__" : "__editor-window_active__");
        }
    }

    public Editor createNewEditor() {
        var newEditor = editorCreator.get();
        newEditor._setEditorWindowInternal(this);
        var button = createEditorButton(newEditor);
        editorButtonContainer.addChild(button);
        addChildAt(newEditor, editors.size());
        editors.put(newEditor, button);
        showEditor(newEditor);
        return newEditor;
    }

    public void removeEditor(Editor editor) {
        var button = editors.remove(editor);
        if (button != null) {
            editorButtonContainer.removeChild(button);
        }
        if (editors.isEmpty()) {
            if (getModularUI() != null && getModularUI().getScreen() != null) {
                getModularUI().getScreen().onClose();
            }
        } else {
            showEditor(editors.lastEntry().getKey());
        }
    }

    protected UIElement createEditorButton(Editor editor) {
        return new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setHeightPercent(100);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setFlex(1);
        }).style(style -> {
            style.setPipelineState(StyleOrigin.DEFAULT);
            style.backgroundTexture(currentEditor == editor ? ColorPattern.SLATE_PLUM.rectTexture() : ColorPattern.DARK_GRAY.rectTexture());
            style.setPipelineState(StyleOrigin.INLINE);
        }).addClass("__editor-window_editor-button__").moveInlineAsDefault().addChildren(
                new TextElement().setText(editor.getTitle()).textStyle(style -> style
                                .textAlignVertical(Vertical.CENTER)
                                .textAlignHorizontal(Horizontal.CENTER)
                                .textWrap(TextWrap.HOVER_ROLL)
                        )
                        .layout(layout -> {
                            layout.setHeightPercent(100);
                            layout.setFlex(1);
                        }).addEventListener(UIEvents.TICK, e -> {
                            if (e.target.getModularUI().getTickCounter() % 20 ==0) {
                                var currentTitle = editor.getTitle();
                                if (e.target instanceof TextElement text && !text.getText().equals(currentTitle)) {
                                    text.setText(currentTitle);
                                }
                            }
                        }).setOverflow(YogaOverflow.HIDDEN),
                new Button().noText().buttonStyle(style -> {
                    style.baseTexture(Icons.REMOVE);
                    style.hoverTexture(Icons.REMOVE.copy().setColor(ColorPattern.GRAY.color));
                    style.pressedTexture(Icons.REMOVE);
                }).setOnClick(e -> {
                    showEditor(editor);
                    editor.exit();
                    e.stopPropagation();
                }).layout(layout -> {
                    layout.setHeight(9);
                    layout.setAspectRatio(1);
                    layout.setMargin(YogaEdge.RIGHT, 2);
                })
        ).addEventListener(UIEvents.MOUSE_DOWN, e -> showEditor(editor));
    }
}
