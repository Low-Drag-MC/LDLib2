package com.lowdragmc.lowdraglib2.editor.ui.view.ui;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
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
import org.appliedenergistics.yoga.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class UIEditorView extends View {
    public final UIElement header = new UIElement();
    public final UIElement canvas = new UIElement();
    public final UIElement editor = new UIElement();
    public final UIHierarchy hierarchy = new UIHierarchy(this);
    public final GraphView graphView = new GraphView();
    public final Inspector inspector = new Inspector();
    public final ModularUIPreview modularUIPreview = new ModularUIPreview(this);
    public final HistoryStack historyStack = new HistoryStack();

    // runtime
    @Nullable @Getter
    private UITemplate template;
    @Nullable @Getter
    private UI currentUI;
    @Nullable @Getter
    private Runnable onTemplateDirty;

    public UIEditorView() {
        super("editor.view.ui_editor");
        // header initial
        header.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeight(16);
            layout.setPadding(YogaEdge.ALL, 1);
            layout.setFlexDirection(YogaFlexDirection.ROW);
        });
        header.style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        header.addChildren(
                // left
                new UIElement().layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setHeightPercent(100);
                    layout.setFlex(1);
                }),
                // center
                new UIElement().layout(layout -> layout.setHeightPercent(100))
                        .addChildren(new Toggle().noText()
                                .setOnToggleChanged(isOn -> {
                                    if (isOn) {
                                        startSimulation();
                                    } else {
                                        stopSimulation();
                                    }
                                })
                                .toggleStyle(style -> style.baseTexture(IGuiTexture.EMPTY)
                                        .unmarkTexture(Icons.PLAY_PAUSE)
                                        .markTexture(Icons.PLAY_PAUSE.copy().setColor(ColorPattern.GREEN.color)))
                                .bindDataSource(SupplierDataSource.of(this::isSimulationRunning), false)
                                .style(style -> style.setTooltips("UIEditor.simulation"))),
                // right
                new UIElement().layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setJustifyContent(YogaJustify.FLEX_END);
                    layout.setHeightPercent(100);
                    layout.setFlex(1);
                }).addChildren(
                        // page fit button
                        new Button().noText().setOnClick(event -> {
                            if (currentUI != null && modularUIPreview.getModularUI() != null) {
                                var modularUI = modularUIPreview.getModularUI();
                                var padding = 5;
                                var x = modularUIPreview.getPositionX() - graphView.getContentX() + modularUI.getLeftPos();
                                var y = modularUIPreview.getPositionY() - graphView.getContentY() + modularUI.getTopPos();
                                var width = modularUI.ui.rootElement.getSizeWidth();
                                var height = modularUI.ui.rootElement.getSizeHeight();
                                graphView.fit(x - padding, y - padding,
                                        x + width + 2 * padding, y + height + 2 * padding,
                                        0.1f);
                            }
                        }).layout(layout -> {
                            layout.setWidth(14);
                        }).style(style -> style.setTooltips("GraphView.fit")).addChild(
                                new UIElement().layout(layout -> {
                                    layout.setHeight(10);
                                    layout.setWidth(10);
                                }).style(style -> style.backgroundTexture(Icons.PAGE_FIT))),
                        // selection box toggle
                        new Toggle()
                                .setText("")
                                .setOn(modularUIPreview.isShowSelectionBox(), false)
                                .toggleButton(button -> button.layout(layout -> {
                                    layout.setWidthPercent(100);
                                    layout.setHeightPercent(100);
                                }))
                                .setOnToggleChanged(modularUIPreview::setShowSelectionBox)
                                .toggleStyle(style -> {
                                    style.baseTexture(Sprites.BORDER1_RT1_DARK);
                                    style.hoverTexture(Sprites.BORDER1_RT1);
                                    style.unmarkTexture(Icons.INFORMATION.copy().scale(0.6f));
                                    style.markTexture(Icons.INFORMATION.copy().setColor(ColorPattern.GRAY.color).scale(0.6f));
                                })
                                .bindDataSource(SupplierDataSource.of(modularUIPreview::isShowSelectionBox))
                                .layout(layout -> {
                                    layout.setPadding(YogaEdge.ALL, 0);
                                    layout.setHeightPercent(100);
                                    layout.setAspectRatio(1f);
                                })
                                .style(style -> style.setTooltips("UIEditor.selection_box"))
                )
        );

        // canvas initial
        canvas.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
        });
        canvas.setDisplay(YogaDisplay.NONE);

        // editor initial
        editor.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWidthPercent(100);
            layout.setFlex(1);
        });

        hierarchy.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });

        graphView.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });
        graphView.addContentChild(modularUIPreview);
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

        editor.addChildren(new SplitView.Horizontal().setPercentage(20)
                .left(hierarchy)
                .right(new SplitView.Horizontal().setPercentage(64)
                        .left(graphView)
                        .right(inspector)));

        setFocusable(true);
        addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);
        addEventListener(UIEvents.BLUR, this::onBlur, true);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown, true);

        addChildren(header, canvas, editor);
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

    public UIEditorView clear() {
        this.stopSimulation();
        this.modularUIPreview.clear();
        this.hierarchy.clearUI();
        this.historyStack.clearHistory();
        this.template = null;
        this.currentUI = null;
        this.onTemplateDirty = null;
        return this;
    }

    public UIEditorView loadUI(@Nonnull UI ui) {
        clear();
        this.currentUI = ui;
        this.modularUIPreview.setModularUI(ui);
        this.hierarchy.loadUI(ui);
        this.modularUIPreview.initPreviewSize((int) graphView.getContentWidth(), (int) graphView.getContentHeight());
        return this;
    }

    public UIEditorView loadTemplate(@Nonnull UITemplate template, Runnable onTemplateDirty) {
        loadUI(template.createUI());
        this.template = template;
        this.onTemplateDirty = onTemplateDirty;
        return this;
    }

    public boolean isSimulationRunning() {
        return canvas.isDisplayed();
    }

    public void startSimulation() {
        if (currentUI == null) return;
        checkTemplateDirtyAndSave();
        var ui = Objects.requireNonNullElseGet(template, () -> currentUI.toTemplate()).createUI();
        canvas.addChildren(ui.rootElement);

        canvas.setDisplay(YogaDisplay.FLEX);
        editor.setDisplay(YogaDisplay.NONE);
    }

    public void stopSimulation() {
        canvas.clearAllChildren();

        canvas.setDisplay(YogaDisplay.NONE);
        editor.setDisplay(YogaDisplay.FLEX);
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

    public void checkTemplateDirtyAndSave() {
        if (template != null && currentUI != null) {
            var newTemplate = currentUI.toTemplate();
            if (newTemplate.getData().equals(template.getData())) return;
            template.setData(newTemplate.getData());
            if (onTemplateDirty != null) {
                onTemplateDirty.run();
            }
        }
    }

    @Override
    public void screenTick() {
        super.screenTick();
        var mui = getModularUI();
        if (mui != null && !isSimulationRunning() && (mui.getTickCounter() & 20) ==0 && (isFocused() || isChildFocused())) {
            checkTemplateDirtyAndSave();
        }
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
        checkTemplateDirtyAndSave();
    }
}
