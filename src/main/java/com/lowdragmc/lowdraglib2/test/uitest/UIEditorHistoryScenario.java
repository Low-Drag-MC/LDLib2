package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.editor.view.UIEditorView;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Undo / redo in the UI editor covers the hierarchy's structural edits, and keeps element identity across
 * them: undoing a property edit on a container must not rebuild its children, or every later history entry
 * that acts on one of them is left pointing at a detached copy.
 */
@LDLRegisterClient(name = "ui_editor_history", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class UIEditorHistoryScenario implements UIScenario {
    private static final String VIEW = "view";
    private static final String A = "a", B = "b", C = "c", PASTED = "pasted";
    private static final String TAB = "tab", TAB_CONTENT = "tab_content", PASTED_TAB = "pasted_tab", PASTED_CONTENT = "pasted_content";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("editor", "history").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("ui_editor", UIEditorHistoryScenario::buildUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .frames(3)
                .step("remember the document's elements", ctx -> {
                    ctx.put(A, doc(ctx, "a"));
                    ctx.put(B, doc(ctx, "b"));
                    ctx.put(C, doc(ctx, "c"));
                })
                .checkEquals("the panel starts with a, b, c", List.of("a", "b", "c"), UIEditorHistoryScenario::panelIds)

                .group("removing the first edit made can be undone and redone", g -> g
                        .step("select a", ctx -> view(ctx).focusElement(ctx.get(A)))
                        .step("remove the selection", ctx -> view(ctx).hierarchy.removeSelected())
                        .checkEquals("a is gone", List.of("b", "c"), UIEditorHistoryScenario::panelIds)
                        .step("focus the editor", ctx -> view(ctx).focus())
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .checkEquals("undo puts a back first", List.of("a", "b", "c"), UIEditorHistoryScenario::panelIds)
                        .check("as the same element", ctx -> panel(ctx).getChildren().getFirst() == ctx.get(A))
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .checkEquals("redo removes it again", List.of("b", "c"), UIEditorHistoryScenario::panelIds)
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .checkEquals("and undo restores it", List.of("a", "b", "c"), UIEditorHistoryScenario::panelIds))

                .group("paste can be undone and redone, and undoing it keeps the panel's children", g -> g
                        .step("copy b", ctx -> {
                            view(ctx).focusElement(ctx.get(B));
                            view(ctx).hierarchy.copySelected();
                        })
                        .step("paste into the panel", ctx -> {
                            view(ctx).focusElement(panel(ctx));
                            view(ctx).hierarchy.pasteToSelected();
                            ctx.put(PASTED, panel(ctx).getChildren().getLast());
                        })
                        .checkEquals("the copy is appended", List.of("a", "b", "c", "b"), UIEditorHistoryScenario::panelIds)
                        .step("focus the editor", ctx -> view(ctx).focus())
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .checkEquals("undo takes the copy out", List.of("a", "b", "c"), UIEditorHistoryScenario::panelIds)
                        .check("and leaves the original children in place", ctx -> sameChildren(ctx, A, B, C))
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .check("redo brings the same copy back", ctx -> panel(ctx).getChildren().getLast() == ctx.get(PASTED))
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL))

                .group("a property edit on the container and a structural edit undo and redo together", g -> g
                        .step("rename the panel through its history recorder, as the inspector does", ctx -> {
                            var panel = panel(ctx);
                            panel.setId("panel_renamed");
                            panel.createHistoryRecorder().record(view(ctx).historyStack, Component.literal("rename"), "rename");
                        })
                        .step("remove c", ctx -> {
                            view(ctx).focusElement(ctx.get(C));
                            view(ctx).hierarchy.removeSelected();
                        })
                        .checkEquals("c is gone", List.of("a", "b"), UIEditorHistoryScenario::panelIds)
                        .step("focus the editor", ctx -> view(ctx).focus())
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .checkEquals("undo restores the panel's name", "panel", ctx -> panel(ctx).getId())
                        .check("without rebuilding its children", ctx -> sameChildren(ctx, A, B, C))
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .checkEquals("redo renames it again", "panel_renamed", ctx -> panel(ctx).getId())
                        .checkEquals("and removes c from the live panel", List.of("a", "b"), UIEditorHistoryScenario::panelIds)
                        .check("c itself is detached", ctx -> ctx.<UIElement>get(C).getParent() == null))

                .group("a property edit right after a paste undoes on its own", g -> g
                        .step("copy a", ctx -> {
                            view(ctx).focusElement(ctx.get(A));
                            view(ctx).hierarchy.copySelected();
                        })
                        .step("paste into the panel", ctx -> {
                            view(ctx).focusElement(panel(ctx));
                            view(ctx).hierarchy.pasteToSelected();
                        })
                        .step("rename the panel while it is still the one inspected", ctx -> {
                            var panel = panel(ctx);
                            panel.setId("panel_edited");
                            panel.createHistoryRecorder().record(view(ctx).historyStack, Component.literal("rename"), "rename");
                        })
                        .step("focus the editor", ctx -> view(ctx).focus())
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .checkEquals("undo takes the rename back", "panel_renamed", ctx -> panel(ctx).getId())
                        .checkEquals("and leaves the paste", List.of("a", "b", "a"), UIEditorHistoryScenario::panelIds)
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .checkEquals("the next undo takes the paste back", List.of("a", "b"), UIEditorHistoryScenario::panelIds)
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .checkEquals("redo pastes again", List.of("a", "b", "a"), UIEditorHistoryScenario::panelIds)
                        .checkEquals("and renames again", "panel_edited", ctx -> panel(ctx).getId()))

                .group("removing a loaded tab takes its content along, and undo brings both back", g -> g
                        .step("remember the tab and its content", ctx -> {
                            var entry = tabs(ctx).getTabContents().entrySet().iterator().next();
                            ctx.put(TAB, entry.getKey());
                            ctx.put(TAB_CONTENT, entry.getValue());
                        })
                        .check("the content was loaded with what is inside it",
                                ctx -> ctx.<UIElement>get(TAB_CONTENT).getChildren().size() == 1)
                        .step("remove the tab", ctx -> {
                            view(ctx).focusElement(ctx.get(TAB));
                            view(ctx).hierarchy.removeSelected();
                        })
                        .check("the tab is gone", ctx -> tabs(ctx).getTabContents().isEmpty())
                        .check("and its content with it", ctx -> ctx.<UIElement>get(TAB_CONTENT).getParent() == null)
                        .step("focus the editor", ctx -> view(ctx).focus())
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .check("undo puts the tab back with the same content", UIEditorHistoryScenario::tabHasItsContent)
                        .check("selected, as it was", ctx -> tabs(ctx).getSelectedTab() == ctx.get(TAB))
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .check("redo removes both again", ctx -> ctx.<UIElement>get(TAB_CONTENT).getParent() == null)
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .check("and undo restores them", UIEditorHistoryScenario::tabHasItsContent))

                .group("redoing a pasted tab brings its content back", g -> g
                        .step("copy the tab", ctx -> {
                            view(ctx).focusElement(ctx.get(TAB));
                            view(ctx).hierarchy.copySelected();
                        })
                        .step("paste it into the tab view", ctx -> {
                            var tabs = tabs(ctx);
                            view(ctx).focusElement(tabs);
                            view(ctx).hierarchy.pasteToSelected();
                            var pasted = tabs.tabScroller.viewContainer.getChildren().getLast();
                            ctx.put(PASTED_TAB, pasted);
                            ctx.put(PASTED_CONTENT, tabs.getTabContents().get(pasted));
                        })
                        .check("the pasted tab has a content", ctx -> ctx.get(PASTED_CONTENT) != null)
                        .step("focus the editor", ctx -> view(ctx).focus())
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .check("undo takes the tab out with its content", ctx ->
                                !tabs(ctx).getTabContents().containsKey(ctx.<Tab>get(PASTED_TAB))
                                        && ctx.<UIElement>get(PASTED_CONTENT).getParent() == null)
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .check("redo puts both back", ctx ->
                                tabs(ctx).getTabContents().get(ctx.<Tab>get(PASTED_TAB)) == ctx.get(PASTED_CONTENT)
                                        && ctx.<UIElement>get(PASTED_CONTENT).getParent() == tabs(ctx).tabContentContainer))

                .closeScreen();
    }

    private static ModularUI buildUI(TestContext ctx) {
        var panel = new UIElement().setId("panel").addChildren(
                new UIElement().setId("a"), new UIElement().setId("b"), new UIElement().setId("c"));
        var tabs = new TabView();
        tabs.addTab(new Tab().setText("one"), new UIElement().addChild(new UIElement().setId("inside")));
        var root = new UIElement().setId("doc").addChildren(panel, tabs);
        var view = new UIEditorView().loadTemplate(UITemplate.of(root), template -> {});
        view.layout(layout -> layout.widthPercent(100).heightPercent(100));
        ctx.put(VIEW, view);
        return new ModularUI(UI.of(view, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MODERN))), ctx.player());
    }

    private static UIEditorView view(TestContext ctx) {
        return ctx.get(VIEW);
    }

    private static UIElement panel(TestContext ctx) {
        var root = view(ctx).getCurrentUI().rootElement;
        return root.getChildren().getFirst();
    }

    private static TabView tabs(TestContext ctx) {
        return (TabView) view(ctx).getCurrentUI().rootElement.getChildren().get(1);
    }

    private static boolean tabHasItsContent(TestContext ctx) {
        var content = ctx.<UIElement>get(TAB_CONTENT);
        return tabs(ctx).getTabContents().get(ctx.<Tab>get(TAB)) == content
                && content.getParent() == tabs(ctx).tabContentContainer;
    }

    private static List<String> panelIds(TestContext ctx) {
        return panel(ctx).getChildren().stream().map(UIElement::getId).toList();
    }

    private static boolean sameChildren(TestContext ctx, String... keys) {
        var children = panel(ctx).getChildren();
        if (children.size() != keys.length) return false;
        for (int i = 0; i < keys.length; i++) {
            if (children.get(i) != ctx.get(keys[i])) return false;
        }
        return true;
    }

    @Nullable
    private static UIElement doc(TestContext ctx, String id) {
        for (var child : panel(ctx).getChildren()) {
            if (child.getId().equals(id)) return child;
        }
        return null;
    }
}
