package com.lowdragmc.lowdraglib2.editor.ui.view.ui;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class UIEditorView extends View {
    public final UIElement header = new UIElement();
    public final UIElement canvas = new UIElement();
    public final UIElement editor = new UIElement();
    public final UIElement styleView = new UIElement();
    public final UIHierarchy hierarchy = new UIHierarchy(this);
    public final GraphView graphView = new GraphView();
    public final CodeEditor stylesheetEditor = new CodeEditor();
    public final Inspector inspector = new Inspector();
    public final ModularUIPreview modularUIPreview = new ModularUIPreview(this);
    public final HistoryStack historyStack = new HistoryStack();
    private final Button saveButton = new Button();
    // runtime
    private boolean isDirty;
    @Nullable @Getter
    private UITemplate template;
    @Nullable @Getter
    private UI currentUI;
    @Nullable @Getter
    private Consumer<UITemplate> onTemplateSaved;
    @Getter
    private boolean isEditingBuiltinStyles;

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
                }).addChildren(
                        saveButton.setOnClick(e -> notifySaved())
                                .setText("ldlib.gui.editor.menu.save")
                                .textStyle(style -> style.textColor(ColorPattern.GRAY.color))
                ),
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
                                .setValue(isSimulationRunning(), false)
                                .toggleStyle(style -> style.baseTexture(IGuiTexture.EMPTY)
                                        .unmarkTexture(Icons.PLAY.copy().setColor(ColorPattern.GREEN.color))
                                        .markTexture(Icons.STOP.copy().setColor(ColorPattern.BRIGHT_RED.color)))
                                .bindDataSource(SupplierDataSource.of(this::isSimulationRunning), false)
                                .style(style -> style.tooltips("UIEditor.simulation"))),
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
                        }).style(style -> style.tooltips("GraphView.fit")).addChild(
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
                                    style.unmarkTexture(Icons.INFORMATION.copy().setColor(ColorPattern.GRAY.color).scale(0.6f));
                                    style.markTexture(Icons.INFORMATION.copy().scale(0.6f));
                                })
                                .bindDataSource(SupplierDataSource.of(modularUIPreview::isShowSelectionBox))
                                .layout(layout -> {
                                    layout.setPadding(YogaEdge.ALL, 0);
                                    layout.setHeightPercent(100);
                                    layout.setAspectRatio(1f);
                                })
                                .style(style -> style.tooltips("UIEditor.selection_box"))
                )
        );

        saveButton.setActive(false);

        // canvas initial
        canvas.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
        });
        canvas.setOverflow(false);
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

        // stylesheet
        styleView.layout(layout -> layout.setHeightPercent(100).setPadding(YogaEdge.ALL, 4));
        styleView.style(style -> style.backgroundTexture(Sprites.BORDER));
        styleView.addChildren(
                new UIElement()
                        .layout(layout -> layout.setHeight(16)
                                .setAlignItems(YogaAlign.CENTER)
                                .setPadding(YogaEdge.ALL, 2)
                                .setFlexDirection(YogaFlexDirection.ROW))
                        .style(style -> style.backgroundTexture(GuiTextureGroup.of(Sprites.RECT_RD_SOLID,
                                DynamicTexture.of(() -> isEditingBuiltinStyles ? Sprites.RECT_RD_T_SOLID : IGuiTexture.EMPTY))))
                        .addChildren(
                                new Label().setText("builtin_styles").textStyle(style -> style
                                                .textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL))
                                        .layout(layout -> layout.setFlex(1)).setOverflow(YogaOverflow.HIDDEN),
                                new UIElement()
                                        .layout(layout -> layout.setHeightPercent(100).setAspectRatio(1))
                                        .style(style -> style.backgroundTexture(DynamicTexture.of(() ->
                                                isEditingBuiltinStyles ? Icons.SETTINGS : IGuiTexture.EMPTY)))
                        ).addEventListener(UIEvents.CLICK, event -> editBuiltinStyles())
        );

        stylesheetEditor.setLanguage(Languages.CSS);
        stylesheetEditor.setActive(false);
        stylesheetEditor.contentView.layout(layout -> layout.setPadding(YogaEdge.ALL, 2));
        stylesheetEditor.contentView.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        stylesheetEditor.textAreaStyle(style -> style.focusOverlay(IGuiTexture.EMPTY));
        stylesheetEditor.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 2);
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });
        stylesheetEditor.style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        stylesheetEditor.setLinesResponder(this::onStylesheetChanged);

        inspector.setHistoryStack(historyStack);
        inspector.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });
        inspector.scrollerView.viewPort.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 5);
        }).style(style -> style.backgroundTexture(Sprites.BORDER));

        editor.addChildren(new SplitView.Horizontal().setPercentage(20)
                .left(new SplitView.Vertical().setPercentage(20)
                        .top(styleView)
                        .bottom(hierarchy))
                .right(new SplitView.Horizontal().setPercentage(64)
                        .left(new SplitView.Horizontal().setPercentage(20)
                                .left(stylesheetEditor)
                                .right(graphView))
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
        this.onTemplateSaved = null;
        this.isEditingBuiltinStyles = false;
        this.stylesheetEditor.setActive(true);
        this.stylesheetEditor.setValue(new String[0]);
        clearDirty();
        return this;
    }

    public UIEditorView loadTemplate(@Nonnull UITemplate template, Consumer<UITemplate> onTemplateSaved) {
        clear();
        this.template = template.copy();
        this.currentUI = this.template.createUI();
        this.modularUIPreview.setModularUI(currentUI);
        this.hierarchy.loadUI(currentUI);
        this.modularUIPreview.initPreviewSize((int) graphView.getContentWidth(), (int) graphView.getContentHeight());
        this.onTemplateSaved = onTemplateSaved;
        editBuiltinStyles();
        return this;
    }

    public boolean isSimulationRunning() {
        return canvas.isDisplayed();
    }

    public void editBuiltinStyles() {
        if (isEditingBuiltinStyles || this.template == null) return;
        isEditingBuiltinStyles = true;
        this.stylesheetEditor.setActive(true);
        this.stylesheetEditor.setValue(Optional.ofNullable(this.template.getBuiltinStyles()).orElse("").split("\n"), false);
    }

    private void onStylesheetChanged(String[] lines) {
        if (this.template == null) return;
        var rawStyle = Arrays.stream(lines).reduce("", (a, b) -> a + b + "\n");
        if (Objects.equals(rawStyle, this.template.getBuiltinStyles())) return;
        this.template.setBuiltinStyles(rawStyle);
        markAsDirty();
        reloadStyles();
    }

    private void reloadStyles() {
        var modularUI = modularUIPreview.getModularUI();
        if (modularUI != null && this.template != null) {
            var styleEngine = modularUI.getStyleEngine();
            styleEngine.clearAllStylesheets();
            styleEngine.addStylesheets(this.template.getAllStylesheets());
        }
    }

    /**
     * Starts the simulation mode for the user interface.
     */
    public void startSimulation() {
        if (currentUI == null || this.template == null) return;
        var ui = currentUI.toTemplate().createUI();

        canvas.addChildren(ui.rootElement);
        canvas.setDisplay(YogaDisplay.FLEX);
        editor.setDisplay(YogaDisplay.NONE);

        // apply styles
        var stylesheets =  this.template.getAllStylesheets();
        var allElements = new ArrayList<>(ui.rootElement.getFlattenChildren());
        allElements.addFirst(ui.rootElement);
        for (Stylesheet stylesheet : stylesheets) {
            for (var flattenChild : allElements) {
                flattenChild.removeAllRules();
                flattenChild.addStyleRules(stylesheet.calculateValues(flattenChild));
            }
        }
    }

    /**
     * Stops the simulation mode for the user interface and transitions the editor UI back to its editing state.
     */
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

    public boolean isTemplateDirty() {
        if (isDirty) return true;
        if (template != null && currentUI != null) {
            var newTemplate = currentUI.toTemplate();
            return !newTemplate.getData().equals(template.getData());
        }
        return false;
    }

    public void markAsDirty() {
        isDirty = true;
        saveButton.setActive(true);
        saveButton.textStyle(style -> style.textColor(ColorPattern.WHITE.color));
    }

    public void clearDirty() {
        isDirty = false;
        saveButton.setActive(false);
        saveButton.textStyle(style -> style.textColor(ColorPattern.GRAY.color));
    }

    public void notifySaved() {
        if (isDirty && template != null && currentUI != null && onTemplateSaved != null) {
            template.setData(currentUI.toTemplate().getData());
            onTemplateSaved.accept(template.copy());
        }
        clearDirty();
    }

    @Override
    public void screenTick() {
        super.screenTick();
        var mui = getModularUI();
        if (!isDirty && mui != null && !isSimulationRunning() && (mui.getTickCounter() & 20) ==0) {
            if (isTemplateDirty()) {
                markAsDirty();
            }
        }
    }

    @Override
    protected Component getViewName() {
        var viewName = Component.translatable(getName());
        if (isDirty) {
            return viewName.append(" *");
        }
        return viewName;
    }

    @Override
    public Tab createTab() {
        var tab = super.createTab();
        return tab.setDynamicText(this::getViewName);
    }

    @Override
    protected void onClose() {
        if (isTemplateDirty()) {
            Dialog.showCheckBox("", "view.save_before_close.info", save -> {
                if (isCanRemove()) {
                    if (save) {
                        notifySaved();
                    }
                    removeSelf();
                }
            }).show(getModularUI());
        } else {
            removeSelf();
        }
    }
}
