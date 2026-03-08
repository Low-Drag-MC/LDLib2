package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.gui.editor.view.ModularUIPreview;
import com.lowdragmc.lowdraglib2.gui.editor.view.UICanvas;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ButtonRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlotRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphViewRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlotRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBarRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SceneRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponentRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SelectorRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitViewRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextAreaRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextFieldRenderer;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphPanel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.WireElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
public final class UIElementClientRenderers {
    private static boolean initialized;

    private UIElementClientRenderers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        UIElementRendererBootstrap.applyRegistry(LDLib2Registries.UI_ELEMENT_RENDERER_ENTRIES);
    }

    private abstract static class LegacyRenderer<T extends UIElement, S extends LegacyRenderer<T, S>> implements RegisteredUIElementRenderer<T, S> {
        protected BiConsumer<T, GUIContext> backgroundAdditional() {
            return null;
        }

        protected BiConsumer<T, GUIContext> backgroundOverlay() {
            return null;
        }

        @Override
        public void drawBackgroundAdditional(T element, IGUIContext context) {
            var additional = backgroundAdditional();
            if (additional != null && context instanceof GUIContext guiContext) {
                additional.accept(element, guiContext);
                return;
            }
            RegisteredUIElementRenderer.super.drawBackgroundAdditional(element, context);
        }

        @Override
        public void drawBackgroundOverlay(T element, IGUIContext context) {
            var overlay = backgroundOverlay();
            if (overlay != null && context instanceof GUIContext guiContext) {
                overlay.accept(element, guiContext);
                return;
            }
            RegisteredUIElementRenderer.super.drawBackgroundOverlay(element, context);
        }
    }

    @LDLRegisterClient(name = "button", registry = "ldlib2:ui_element_renderer")
    public static final class ButtonRendererRegistration extends LegacyRenderer<Button, ButtonRendererRegistration> {
        @Override public Class<Button> type() { return Button.class; }
        @Override protected BiConsumer<Button, GUIContext> backgroundAdditional() { return ButtonRenderer::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "dialog", registry = "ldlib2:ui_element_renderer")
    public static final class DialogRenderer extends LegacyRenderer<Dialog, DialogRenderer> {
        @Override public Class<Dialog> type() { return Dialog.class; }
        @Override protected BiConsumer<Dialog, GUIContext> backgroundAdditional() { return Dialog::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "fluid_slot", registry = "ldlib2:ui_element_renderer")
    public static final class FluidSlotRendererRegistration extends LegacyRenderer<FluidSlot, FluidSlotRendererRegistration> {
        @Override public Class<FluidSlot> type() { return FluidSlot.class; }
        @Override protected BiConsumer<FluidSlot, GUIContext> backgroundAdditional() { return FluidSlotRenderer::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "graph_panel", registry = "ldlib2:ui_element_renderer")
    public static final class GraphPanelRenderer extends LegacyRenderer<GraphPanel, GraphPanelRenderer> {
        @Override public Class<GraphPanel> type() { return GraphPanel.class; }
        @Override protected BiConsumer<GraphPanel, GUIContext> backgroundAdditional() { return GraphPanel::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "graph_view", registry = "ldlib2:ui_element_renderer")
    public static final class GraphViewRendererRegistration extends LegacyRenderer<GraphView, GraphViewRendererRegistration> {
        @Override public Class<GraphView> type() { return GraphView.class; }
        @Override protected BiConsumer<GraphView, GUIContext> backgroundAdditional() { return GraphViewRenderer::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "editor_window", registry = "ldlib2:ui_element_renderer")
    public static final class EditorWindowRenderer extends LegacyRenderer<EditorWindow, EditorWindowRenderer> {
        @Override public Class<EditorWindow> type() { return EditorWindow.class; }
        @Override protected BiConsumer<EditorWindow, GUIContext> backgroundAdditional() { return EditorWindow::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "item_slot", registry = "ldlib2:ui_element_renderer")
    public static final class ItemSlotRendererRegistration extends LegacyRenderer<ItemSlot, ItemSlotRendererRegistration> {
        @Override public Class<ItemSlot> type() { return ItemSlot.class; }
        @Override protected BiConsumer<ItemSlot, GUIContext> backgroundAdditional() { return ItemSlotRenderer::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "modular_ui_preview", registry = "ldlib2:ui_element_renderer")
    public static final class ModularUIPreviewRenderer extends LegacyRenderer<ModularUIPreview, ModularUIPreviewRenderer> {
        @Override public Class<ModularUIPreview> type() { return ModularUIPreview.class; }
        @Override protected BiConsumer<ModularUIPreview, GUIContext> backgroundAdditional() { return ModularUIPreview::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "node_element", registry = "ldlib2:ui_element_renderer")
    public static final class NodeElementRenderer extends LegacyRenderer<NodeElement, NodeElementRenderer> {
        @Override public Class<NodeElement> type() { return NodeElement.class; }
        @Override protected BiConsumer<NodeElement, GUIContext> backgroundOverlay() { return NodeElement::drawBackgroundOverlay; }
    }

    @LDLRegisterClient(name = "progress_bar", registry = "ldlib2:ui_element_renderer")
    public static final class ProgressBarRendererRegistration extends LegacyRenderer<ProgressBar, ProgressBarRendererRegistration> {
        @Override public Class<ProgressBar> type() { return ProgressBar.class; }
        @Override protected BiConsumer<ProgressBar, GUIContext> backgroundAdditional() { return ProgressBarRenderer::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "scene", registry = "ldlib2:ui_element_renderer")
    public static final class SceneRendererRegistration extends LegacyRenderer<Scene, SceneRendererRegistration> {
        @Override public Class<Scene> type() { return Scene.class; }
        @Override protected BiConsumer<Scene, GUIContext> backgroundAdditional() { return SceneRenderer::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "scene_editor", registry = "ldlib2:ui_element_renderer")
    public static final class SceneEditorRenderer extends LegacyRenderer<SceneEditor, SceneEditorRenderer> {
        @Override public Class<SceneEditor> type() { return SceneEditor.class; }
        @Override protected BiConsumer<SceneEditor, GUIContext> backgroundAdditional() { return SceneEditor::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "search_component", registry = "ldlib2:ui_element_renderer")
    public static final class SearchComponentRendererRegistration extends LegacyRenderer<SearchComponent, SearchComponentRendererRegistration> {
        @Override public Class<SearchComponent> type() { return SearchComponent.class; }
        @Override protected BiConsumer<SearchComponent, GUIContext> backgroundOverlay() { return SearchComponentRenderer::drawBackgroundOverlay; }
    }

    @LDLRegisterClient(name = "selector", registry = "ldlib2:ui_element_renderer")
    public static final class SelectorRendererRegistration extends LegacyRenderer<Selector, SelectorRendererRegistration> {
        @Override public Class<Selector> type() { return Selector.class; }
        @Override protected BiConsumer<Selector, GUIContext> backgroundOverlay() { return SelectorRenderer::drawBackgroundOverlay; }
    }

    @LDLRegisterClient(name = "split_view", registry = "ldlib2:ui_element_renderer")
    public static final class SplitViewRendererRegistration extends LegacyRenderer<SplitView, SplitViewRendererRegistration> {
        @Override public Class<SplitView> type() { return SplitView.class; }
        @Override protected BiConsumer<SplitView, GUIContext> backgroundAdditional() { return SplitViewRenderer::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "tab", registry = "ldlib2:ui_element_renderer")
    public static final class TabRendererRegistration extends LegacyRenderer<Tab, TabRendererRegistration> {
        @Override public Class<Tab> type() { return Tab.class; }
        @Override protected BiConsumer<Tab, GUIContext> backgroundAdditional() { return TabRenderer::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "text_area", registry = "ldlib2:ui_element_renderer")
    public static final class TextAreaRendererRegistration extends LegacyRenderer<TextArea, TextAreaRendererRegistration> {
        @Override public Class<TextArea> type() { return TextArea.class; }
        @Override protected BiConsumer<TextArea, GUIContext> backgroundOverlay() { return TextAreaRenderer::drawBackgroundOverlay; }
    }

    @LDLRegisterClient(name = "text_area_content_view", registry = "ldlib2:ui_element_renderer")
    public static final class TextAreaContentViewRenderer extends LegacyRenderer<TextArea.ContentView, TextAreaContentViewRenderer> {
        @Override public Class<TextArea.ContentView> type() { return TextArea.ContentView.class; }
        @Override protected BiConsumer<TextArea.ContentView, GUIContext> backgroundAdditional() { return TextAreaRenderer::drawContentViewElement; }
    }

    @LDLRegisterClient(name = "text_element", registry = "ldlib2:ui_element_renderer")
    public static final class TextElementRendererRegistration extends LegacyRenderer<TextElement, TextElementRendererRegistration> {
        @Override public Class<TextElement> type() { return TextElement.class; }
        @Override protected BiConsumer<TextElement, GUIContext> backgroundAdditional() { return TextElementRenderer::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "text_field", registry = "ldlib2:ui_element_renderer")
    public static final class TextFieldRendererRegistration extends LegacyRenderer<TextField, TextFieldRendererRegistration> {
        @Override public Class<TextField> type() { return TextField.class; }
        @Override protected BiConsumer<TextField, GUIContext> backgroundAdditional() { return TextFieldRenderer::drawBackgroundAdditional; }
        @Override protected BiConsumer<TextField, GUIContext> backgroundOverlay() { return TextFieldRenderer::drawBackgroundOverlay; }
    }

    @LDLRegisterClient(name = "ui_canvas", registry = "ldlib2:ui_element_renderer")
    public static final class UICanvasRenderer extends LegacyRenderer<UICanvas, UICanvasRenderer> {
        @Override public Class<UICanvas> type() { return UICanvas.class; }
        @Override protected BiConsumer<UICanvas, GUIContext> backgroundAdditional() { return UICanvas::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "ui_debugger", registry = "ldlib2:ui_element_renderer")
    public static final class UIDebuggerRenderer extends LegacyRenderer<UIDebugger, UIDebuggerRenderer> {
        @Override public Class<UIDebugger> type() { return UIDebugger.class; }
        @Override protected BiConsumer<UIDebugger, GUIContext> backgroundAdditional() { return UIDebugger::drawBackgroundAdditional; }
    }

    @LDLRegisterClient(name = "wire_element", registry = "ldlib2:ui_element_renderer")
    public static final class WireElementRenderer extends LegacyRenderer<WireElement, WireElementRenderer> {
        @Override public Class<WireElement> type() { return WireElement.class; }
        @Override protected BiConsumer<WireElement, GUIContext> backgroundAdditional() { return WireElement::drawBackgroundAdditional; }
    }
}
