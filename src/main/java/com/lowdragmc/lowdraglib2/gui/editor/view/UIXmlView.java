package com.lowdragmc.lowdraglib2.gui.editor.view;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.editor.UIXmlProject;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UIXmlView extends View {
    public final UIXmlProject project;
    public final UIElement header = new UIElement();
    public final UICanvas canvas = new UICanvas();
    public final UIElement editor = new UIElement();
    public final GraphView graphView = new GraphView();
    public final CodeEditor xmlEditor = new CodeEditor();
    public final ModularUIPreview modularUIPreview = new ModularUIPreview(null);

    // runtime
    private long lastModify;
    private boolean isXmlDirty;
    @Nullable
    private UI ui;
    @Nullable
    private Document document;

    public UIXmlView(UIXmlProject project) {
        super("editor.view.ui_xml_editor");
        this.setCanRemove(false);
        this.project = project;

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
                }).addChildren(),
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
                            if (ui != null && modularUIPreview.getPreviewModularUI() != null) {
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
                                }).style(style -> style.backgroundTexture(Icons.PAGE_FIT)).addClass("__white_icon__")),
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
                                .style(style -> style.tooltips("UIEditor.selection_box"))
                )
        );
        header.addClass("__ui-editor-view_header__").moveInlineAsDefault();

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

        graphView.layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
        });
        graphView.addContentChild(modularUIPreview);
        graphView.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            modularUIPreview.initPreviewSize((int) graphView.getContentWidth(), (int) graphView.getContentHeight());
        });
        graphView.addClass("__ui-editor-view_graph-view__").moveInlineAsDefault();

        xmlEditor.setLanguage(Languages.XML);
        xmlEditor.contentView.layout(layout -> layout.paddingAll(2));
        xmlEditor.contentView.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        xmlEditor.textAreaStyle(style -> style.focusOverlay(IGuiTexture.EMPTY));
        xmlEditor.layout(layout -> {
            layout.paddingAll(2);
            layout.heightPercent(100);
            layout.widthPercent(100);
        });
        xmlEditor.style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        xmlEditor.setLinesResponder(this::onXmlChanged);
        xmlEditor.getStyleBag().moveInlineAsDefault();
        xmlEditor.addClass("__ui-editor-view_stylesheet-editor__").moveInlineAsDefault();

        editor.addChildren(new SplitView.Horizontal().setPercentage(50)
                .left(xmlEditor)
                .right(graphView));

        dynamicName = () -> {
            var editor =  project.getEditor();
            if (editor != null && editor.getCurrentProject() == project && editor.getCurrentProjectFile() != null) {
                return Component.literal(editor.getCurrentProjectFile().getName());
            }
            return project.getDisplayName();
        };
        addChildren(header, canvas, editor);

        loadXmlFromProject();
    }

    protected void onXmlChanged(String[] lines) {
        var xml = String.join("\n", lines);
        if (project.getXml().equals(xml)) return;
        project.setXml(xml, false);
        loadXml(xml);
        isXmlDirty = true;
    }

    protected void loadXml(String xml) {
        this.modularUIPreview.clear();
        this.ui = null;
        this.document = null;
        try(var inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            this.document = XmlUtils.loadXml(inputStream);
            if (this.document != null) {
                this.ui = UI.of(document);
                this.modularUIPreview.loadUI(this.ui);
            }
        } catch (Exception e) {
            if (Platform.isDevEnv()) {
                LDLib2.LOGGER.warn("Could not load xml", e);
            }
        }
    }

    public void loadXmlFromProject() {
        loadXml(project.getXml());
        xmlEditor.setValue(project.getXml().lines().toArray(String[]::new), false);
        var editor = project.getEditor();
        if (editor != null && editor.getCurrentProject() == project && editor.getCurrentProjectFile() != null) {
            lastModify = editor.getCurrentProjectFile().lastModified();
        }
    }

    public boolean isSimulationRunning() {
        return canvas.isDisplayed() && canvas.isSimulating();
    }

    /**
     * Starts the simulation mode for the user interface.
     */
    public void startSimulation() {
        if (this.document == null) return;
        canvas.startSimulation(UI.of(this.document));
        canvas.setDisplay(true);
        editor.setDisplay(false);
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
    public void screenTick() {
        super.screenTick();
        // check if project dirty per 0.5s
        var editor = project.getEditor();
        if (editor == null) return;
        if (editor.getCurrentProject() != project) return;
        if (editor.getCurrentProjectFile() == null) return;
        var mui = getModularUI();
        if (mui != null && mui.getTickCounter() % 10 == 0) {
            var file = editor.getCurrentProjectFile();
            if (isXmlDirty) {
                isXmlDirty = false;
                editor.saveProject(null, false);
                lastModify = file.lastModified();
            } else {
                // trace changes
                if (lastModify != file.lastModified()) {
                    lastModify = file.lastModified();
                    if (Files.exists(file.toPath())) {
                        try {
                            var rawText = new String(Files.readAllBytes(file.toPath()));
                            project.setXml(rawText);
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
    }
}
