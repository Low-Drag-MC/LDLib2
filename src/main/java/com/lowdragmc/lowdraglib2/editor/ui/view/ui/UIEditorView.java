package com.lowdragmc.lowdraglib2.editor.ui.view.ui;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import lombok.Getter;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UIEditorView extends View {
    public final UIHierarchy hierarchy;
    public final GraphView graphView = new GraphView();
    public final Inspector inspector = new Inspector();
    public final ModularUIPreview modularUIPreview;
    public final HistoryStack historyStack = new HistoryStack();
    // runtime
    @Nullable @Getter
    private UI currentUI;

    public UIEditorView() {
        super("editor.view.ui_editor");
        this.hierarchy = new UIHierarchy(this);
        getLayout().setFlexDirection(YogaFlexDirection.ROW);

        hierarchy.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });

        graphView.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });
        graphView.addContentChild(modularUIPreview = new ModularUIPreview(this));
        graphView.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            modularUIPreview.initPreviewSize((int) graphView.getContentWidth(), (int) graphView.getContentHeight());
        });

        inspector.setHistoryStack(historyStack);
        inspector.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });
        inspector.scrollerView.viewPort.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 5);
        }).style(style -> style.backgroundTexture(Sprites.BORDER));

        addChildren(new SplitView.Horizontal().setPercentage(20)
                .left(hierarchy)
                .right(new SplitView.Horizontal().setPercentage(64)
                        .left(graphView)
                        .right(inspector)));

        setFocusable(true);
        addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);
        addEventListener(UIEvents.BLUR, this::onBlur, true);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown, true);
    }

    protected void onMouseDown(UIEvent event) {
        if (event.target.isFocusable()) return;
        focus();
    }

    protected void onBlur(UIEvent event) {
        if (event.relatedTarget != null && this.isAncestorOf(event.relatedTarget)) { // focus on children
            return;
        }

        if (event.target == this) { // lose focus
            if (isChildHover() && event.relatedTarget == null) {
                focus();
            }
        } else { // child lose focus
            if (event.relatedTarget == null && isChildHover()) {
                focus();
            }
        }
    }

    protected void onValidateCommand(UIEvent event) {
        if (CommandEvents.REDO.equals(event.command) && !historyStack.getRedoStack().isEmpty()) {
            event.stopPropagation();
        }
        if (CommandEvents.UNDO.equals(event.command) && !historyStack.getUndoStack().isEmpty()) {
            event.stopPropagation();
        }
    }

    protected void onExecuteCommand(UIEvent event) {
        if (CommandEvents.REDO.equals(event.command) && !historyStack.getRedoStack().isEmpty()) {
            historyStack.redo();
        }
        if (CommandEvents.UNDO.equals(event.command) && !historyStack.getUndoStack().isEmpty()) {
            historyStack.undo();
        }
    }

    public UIEditorView clearUI() {
        this.modularUIPreview.clear();
        this.hierarchy.clearUI();
        this.historyStack.clearHistory();
        this.currentUI = null;
        return this;
    }

    public UIEditorView loadUI(@Nonnull UI ui) {
        this.currentUI = ui;
        this.modularUIPreview.setModularUI(ui);
        this.hierarchy.loadUI(ui);
        this.modularUIPreview.initPreviewSize((int) graphView.getContentWidth(), (int) graphView.getContentHeight());
        return this;
    }

    public <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, UIElementProvider<T> uiProvider) {
        var menu = new Menu<>(menuNode, uiProvider);
        menu.layout(layout -> {
            layout.setPosition(YogaEdge.LEFT, posX - getContentX());
            layout.setPosition(YogaEdge.TOP, posY - getContentY());
        });
        addChildren(menu);
        return menu;
    }

    public void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder) {
        if (menuBuilder == null || menuBuilder.isEmpty()) return;
        openMenu(posX, posY, menuBuilder.build(), TreeBuilder.Menu::uiProvider)
                .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                .setOnNodeClicked(TreeBuilder.Menu::handle);
    }
}
