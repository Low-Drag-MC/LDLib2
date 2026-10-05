package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.editor.view.UIEditorView;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
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

                .closeScreen();
    }

    private static ModularUI buildUI(TestContext ctx) {
        var panel = new UIElement().setId("panel").addChildren(
                new UIElement().setId("a"), new UIElement().setId("b"), new UIElement().setId("c"));
        var root = new UIElement().setId("doc").addChild(panel);
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
