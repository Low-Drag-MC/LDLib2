package com.lowdragmc.lowdraglib2.gui.editor.view;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class UIEditorView extends View {
    public final UIElement header = new UIElement();
    public final UICanvas canvas = new UICanvas();
    public final UIElement editor = new UIElement();
    public final UIElement styleView = new UIElement();
    public final UIHierarchy hierarchy = new UIHierarchy();
    public final GraphView graphView = new GraphView();
    public final CodeEditor stylesheetEditor = new CodeEditor();
    public final Inspector inspector = new Inspector();
    public final ModularUIPreview modularUIPreview = new ModularUIPreview(this);
    public final HistoryStack historyStack = new HistoryStack();
    public final Button saveButton = new Button();
    // runtime
    private boolean isDirty;
    @Nullable
    @Getter
    private UITemplate template;
    @Nullable
    @Getter
    private UI currentUI;
    @Nullable
    @Getter
    private Consumer<UITemplate> onTemplateSaved;
    @Getter
    private boolean isEditingBuiltinStyles;
    @Setter
    @Getter
    private boolean focusHierarchyFromPreview = true;

    public UIEditorView() {
        super("editor.view.ui_editor");
        addClass("__ui-editor-view__");
        // header initial
        header.layout(layout -> {
            layout.widthPercent(100);
            layout.height(16);
            layout.paddingAll(1);
            layout.flexDirection(FlexDirection.ROW);
        });
        header.style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        header.addChildren(
                // left
                new UIElement().layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.heightPercent(100);
                    layout.flex(1);
                }).addChildren(
                        saveButton.setOnClick(e -> notifySaved())
                                .setText("ldlib.gui.editor.menu.save")
                ),
                // center
                new UIElement().layout(layout -> layout.heightPercent(100))
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
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.FLEX_END);
                    layout.heightPercent(100);
                    layout.flex(1);
                }).addChildren(
                        // page fit button
                        new Button().noText().setOnClick(event -> {
                            if (currentUI != null && modularUIPreview.getPreviewModularUI() != null) {
                                var modularUI = modularUIPreview.getPreviewModularUI();
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
                            layout.width(14);
                        }).style(style -> style.tooltips("GraphView.fit")).addChild(
                                new UIElement().layout(layout -> {
                                    layout.heightPercent(100);
                                    layout.setAspectRatio(1);
                                }).style(style -> style.backgroundTexture(Icons.PAGE_FIT)).addClasses("__white_icon__")),
                        // selection box toggle
                        new Toggle()
                                .setText("")
                                .setOn(modularUIPreview.isShowSelectionBox(), false)
                                .toggleButton(button -> button.layout(layout -> {
                                    layout.widthPercent(100);
                                    layout.heightPercent(100);
                                }))
                                .setOnToggleChanged(modularUIPreview::setShowSelectionBox)
                                .toggleStyle(style -> {
                                    style.setPipelineState(StyleOrigin.DEFAULT);
                                    style.baseTexture(Sprites.BORDER1_RT1_DARK);
                                    style.hoverTexture(Sprites.BORDER1_RT1);
                                    style.setPipelineState(StyleOrigin.INLINE);
                                    style.unmarkTexture(Icons.INFORMATION.copy().setColor(ColorPattern.GRAY.color).scale(0.6f));
                                    style.markTexture(Icons.INFORMATION.copy().scale(0.6f));
                                })
                                .bindDataSource(SupplierDataSource.of(modularUIPreview::isShowSelectionBox))
                                .layout(layout -> {
                                    layout.paddingAll(0);
                                    layout.heightPercent(100);
                                    layout.setAspectRatio(1f);
                                })
                                .style(style -> style.tooltips("UIEditor.selection_box")),
                        new Toggle()
                                .setText("")
                                .setOn(isFocusHierarchyFromPreview(), false)
                                .toggleButton(button -> button.layout(layout -> {
                                    layout.widthPercent(100);
                                    layout.heightPercent(100);
                                }))
                                .setOnToggleChanged(this::setFocusHierarchyFromPreview)
                                .toggleStyle(style -> {
                                    style.setPipelineState(StyleOrigin.DEFAULT);
                                    style.baseTexture(Sprites.BORDER1_RT1_DARK);
                                    style.hoverTexture(Sprites.BORDER1_RT1);
                                    style.setPipelineState(StyleOrigin.INLINE);
                                    style.unmarkTexture(Icons.DOWN_ARROW_NO_BAR.copy().setColor(ColorPattern.GRAY.color).scale(0.8f));
                                    style.markTexture(Icons.DOWN_ARROW_NO_BAR.copy().scale(0.8f));
                                })
                                .bindDataSource(SupplierDataSource.of(this::isFocusHierarchyFromPreview))
                                .layout(layout -> {
                                    layout.paddingAll(0);
                                    layout.heightPercent(100);
                                    layout.setAspectRatio(1f);
                                })
                                .style(style -> style.tooltips("ui_editor.focus_mode.0", "ui_editor.focus_mode.1"))
                )
        );
        header.addClass("__ui-editor-view_header__").moveInlineAsDefault();
        header.setOverflowVisible(false);

        saveButton.setActive(false);

        // canvas initial
        canvas.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        canvas.setOverflowVisible(false);
        canvas.setDisplay(false);
        canvas.addClass("__ui-editor-view_canvas__").moveInlineAsDefault();

        // editor initial
        editor.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.widthPercent(100);
            layout.flex(1);
        });
        editor.addClass("__ui-editor-view_editor__").moveInlineAsDefault();

        hierarchy.layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
        });
        hierarchy.addClass("__ui-editor-view_hierarchy__").moveInlineAsDefault();
        hierarchy.setOnSelectedChanged(this::onHierarchyNodeSelected);

        graphView.layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
        });
        graphView.addContentChild(modularUIPreview);
        graphView.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            modularUIPreview.initPreviewSize((int) graphView.getContentWidth(), (int) graphView.getContentHeight());
        });
        graphView.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (!event.isShiftDown() || event.button != 0 || !this.isFocusHierarchyFromPreview() || this.modularUIPreview.getPreviewModularUI() == null) {
                return;
            }
            var hovered = this.modularUIPreview.getPreviewModularUI().getLastHoveredElement();
            if (hovered != null) {
                this.focusElement(hovered);
            }
            event.stopImmediatePropagation();
        }, true);
        graphView.addClass("__ui-editor-view_graph-view__").moveInlineAsDefault();

        // stylesheet
        var stylesheetSelector = new ArrayConfiguratorGroup<>("", false, () -> {
            if (this.template == null) return List.of();
            return this.template.getStylesheets();
        }, (getter, setter) -> new SearchComponentConfigurator<>("", getter, setter, new SearchComponentConfigurator.ISearchConfigurator<ResourceLocation>() {
            @Override
            @Nonnull
            public ResourceLocation defaultValue() {
                return LDLib2.id("gdp");
            }

            @Override
            @Nonnull
            public String resultText(@NotNull ResourceLocation value) {
                return value.toString();
            }

            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                var lowerWord = word.toLowerCase();
                var candidates = new HashSet<ResourceLocation>();
                for (var key : StylesheetManager.INSTANCE.getAllPackStylesheets()) {
                    if (Thread.currentThread().isInterrupted()) return;
                    candidates.add(key);
                }
                for (var key : StylesheetManager.INSTANCE.getAllBuiltinStylesheets()) {
                    if (Thread.currentThread().isInterrupted()) return;
                    candidates.add(key);
                }
                for (var candidate : candidates) {
                    if (candidate.toString().toLowerCase().contains(lowerWord)) {
                        if (candidate.getPath().endsWith(".lss")) {
                            searchHandler.acceptResult(candidate.withPath(candidate.getPath().substring(0, candidate.getPath().length() - 4)));
                        }
                        searchHandler.acceptResult(candidate);
                    }
                }
            }
        }, true), true);
        stylesheetSelector.setAddDefault(() -> LDLib2.id("lss/gdp.lss"))
                .setOnUpdate(stylesheetList -> {
                    if (this.template != null) {
                        var stylesheets = this.template.getStylesheets();
                        stylesheets.clear();
                        stylesheets.addAll(stylesheetList);
                        reloadStyles();
                        if (stylesheetSelector.getSelected() != null) {
                            viewStylesheet(stylesheetSelector.getSelected().object);
                        } else {
                            editBuiltinStyles();
                        }
                        markAsDirty();
                    }
                })
                .setOnSelectedChanged(selected -> {
                    if (selected != null) {
                        viewStylesheet(selected);
                    } else {
                        editBuiltinStyles();
                    }
                })
                .setCanCollapse(false)
                .configuratorContainer(container -> container.layout(layout -> layout.marginLeft(0)))
                .hideTitle();
        styleView.layout(layout -> layout.heightPercent(100).paddingAll(4));
        styleView.style(style -> style.backgroundTexture(Sprites.BORDER));
        styleView.addChildren(
                new Toggle()
                        .noText()
                        .toggleStyle(toggleStyle -> {
                            toggleStyle.setPipelineState(StyleOrigin.DEFAULT);
                            toggleStyle.baseTexture(IGuiTexture.EMPTY);
                            toggleStyle.hoverTexture(Sprites.RECT_RD_T_SOLID);
                            toggleStyle.unmarkTexture(IGuiTexture.EMPTY);
                            toggleStyle.markTexture(Sprites.RECT_RD_T_SOLID);
                            toggleStyle.setPipelineState(StyleOrigin.INLINE);
                        })
                        .toggleButton(button -> button.setText("builtin_styles")
                                .setOverflowVisible(false)
                                .layout(layout -> layout.setAspectRatioAuto().widthPercent(100)))
                        .toggleButton(button -> button.text
                                .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.LEFT))
                                .setDisplay(true)
                                .layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)))
                        .bindDataSource(SupplierDataSource.of(this::isEditingBuiltinStyles), false)
                        .selfCall(toggle -> ((Toggle) toggle).setOnToggleChanged(isOn -> {
                            if (isOn) {
                                stylesheetSelector.setSelected(null);
                            } else {
                                ((Toggle) toggle).setOn(true, false);
                            }
                        }))
                        .addClass("__ui-editor-view_builtin-styles-toggle__"),
                // style sheet selector
                new ScrollerView().addScrollViewChildren(stylesheetSelector).layout(layout -> layout.widthPercent(100).flex(1))

        );
        styleView.addClass("__ui-editor-view_style-view__").moveInlineAsDefault();


        stylesheetEditor.setLanguage(Languages.LSS);
        stylesheetEditor.setActive(false);
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
        stylesheetEditor.addClass("__ui-editor-view_stylesheet-editor__").moveInlineAsDefault();

        inspector.setHistoryStack(historyStack);
        inspector.layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
        });
        inspector.scrollerView.viewPort.layout(layout -> {
            layout.paddingAll(5);
        }).style(style -> style.backgroundTexture(Sprites.BORDER)).moveInlineAsDefault();
        inspector.addClass("__ui-editor-view_inspector__").moveInlineAsDefault();


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
        dynamicName = () -> Component.translatable(getName());
        addChildren(header, canvas, editor);
    }

    protected void onHierarchyNodeSelected(Set<UITreeNode> selected) {
        if (selected.size() == 1) {
            var element = selected.iterator().next().getKey();
            if (inspector.getInspectedConfigurable() != element) {
                inspector.inspect(element);
            }
        } else {
            inspector.clear();
        }
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
            if (isSelfOrChildHover() && event.relatedTarget == null) {
                focus();
            }
        } else { // child lose focus
            if (event.relatedTarget == null && isSelfOrChildHover()) {
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

    @Override
    protected void onAdded() {
        super.onAdded();
        if (this.template != null) {
            this.loadTemplate(this.template, this.onTemplateSaved);
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
        this.modularUIPreview.loadUI(currentUI);
        this.hierarchy.loadUI(currentUI);
        this.modularUIPreview.initPreviewSize((int) graphView.getContentWidth(), (int) graphView.getContentHeight());
        this.onTemplateSaved = onTemplateSaved;
        editBuiltinStyles();
        return this;
    }

    public boolean isSimulationRunning() {
        return canvas.isDisplayed() && canvas.isSimulating();
    }

    public void editBuiltinStyles() {
        if (isEditingBuiltinStyles || this.template == null) return;
        isEditingBuiltinStyles = true;
        this.stylesheetEditor.setActive(true);
        this.stylesheetEditor.setValue(Optional.ofNullable(this.template.getBuiltinStyles()).orElse("").split("\n"), false);
    }

    public void viewStylesheet(ResourceLocation stylesheet) {
        isEditingBuiltinStyles = false;
        var res = Minecraft.getInstance().getResourceManager().getResource(stylesheet);
        if (res.isPresent()) {
            try (var reader = res.get().openAsReader()) {
                this.stylesheetEditor.setValue(reader.lines().toArray(String[]::new), false);
                this.stylesheetEditor.setActive(false);
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to load style sheet {}", stylesheet, e);
            }
        }
    }

    private void onStylesheetChanged(String[] lines) {
        if (this.template == null) return;
        var rawStyle = lines.length == 0 ? "" : String.join("\n", lines);
        if (Objects.equals(rawStyle, this.template.getBuiltinStyles())) return;
        this.template.setBuiltinStyles(rawStyle);
        markAsDirty();
        reloadStyles();
    }

    private void reloadStyles() {
        var modularUI = modularUIPreview.getPreviewModularUI();
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
        canvas.setDisplay(true);
        editor.setDisplay(false);

        // convert to a real UI with styles applied
        var newTemplate = currentUI.toTemplate();
        newTemplate.copyStylesFrom(this.template);

        canvas.startSimulation(newTemplate.createUI());
    }

    /**
     * Stops the simulation mode for the user interface and transitions the editor UI back to its editing state.
     */
    public void stopSimulation() {
        canvas.setDisplay(false);
        editor.setDisplay(true);
        canvas.stopSimulation();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
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
    }

    public void clearDirty() {
        isDirty = false;
        saveButton.setActive(false);
    }

    public void focusElement(UIElement element) {
        if (hierarchy.getRootNode() != null) {
            var node = findElementNode(hierarchy.getRootNode(), element);
            hierarchy.focusNode(node);
        }
    }

    @Nullable
    private UITreeNode findElementNode(UITreeNode node, UIElement element) {
        if (node.getKey() == element) {
            return node;
        }
        for (UITreeNode child : node.getChildren()) {
            var result = findElementNode(child, element);
            if (result != null) {
                return result;
            }
        }
        return null;
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
        if (!isDirty && mui != null && !isSimulationRunning() && (mui.getTickCounter() & 20) == 0) {
            if (isTemplateDirty()) {
                markAsDirty();
            }
        }
    }

    @Override
    protected Component getViewName() {
        var viewName = super.getViewName();
        if (isDirty) {
            return viewName.copy().append(" *");
        }
        return viewName;
    }

    @Override
    protected void onClose() {
        if (isTemplateDirty()) {
            Dialog.showCancelableCheck("Dialog.notify", "view.save_before_close.info", save -> {
                if (isCanRemove()) {
                    if (save) {
                        notifySaved();
                    }
                    removeSelf();
                }
            }, Runnables.doNothing()).show(getModularUI());
        } else {
            removeSelf();
        }
    }
}
