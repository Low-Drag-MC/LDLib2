package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorLayout;
import com.lowdragmc.lowdraglib2.editor.ui.SplittableWindow;
import com.lowdragmc.lowdraglib2.editor.ui.SplittableWindow.LayoutConfig;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Saved layouts that disagree about the named anchor windows. A layout without the center anchor left
 * {@link Editor#centerWindow} on the root window, so the next layout naming the center rebuilt the root inside
 * itself and overflowed the stack. And the windows a rebuild left behind still pointed into the new tree: when
 * their views moved out, their collapse tore panes, and the views in them, out of it.
 */
@LDLRegisterClient(name = "editor_layout_anchors", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class EditorLayoutAnchorScenario implements UIScenario {

    private static final String VIEWS = "docked_views";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "dock", "layout").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("count the docked views", ctx -> ctx.put(VIEWS, editor(ctx).rootWindow.getAllViews().size()))
                .check("adding an ancestor as a child is refused", ctx -> {
                    var parent = new UIElement();
                    var child = new UIElement();
                    parent.addChild(child);
                    try {
                        child.addChild(parent);
                        return false;
                    } catch (IllegalArgumentException e) {
                        return true;
                    }
                })

                .step("apply a layout whose center pane has no anchor",
                        ctx -> editor(ctx).applyLayout(layout(null, Editor.ANCHOR_RIGHT)))
                .frames(2)
                .check("the window tree is intact", EditorLayoutAnchorScenario::intact)

                .step("apply a layout naming the center pane",
                        ctx -> editor(ctx).applyLayout(layout(Editor.ANCHOR_CENTER, Editor.ANCHOR_RIGHT)))
                .frames(2)
                .check("the window tree is intact", EditorLayoutAnchorScenario::intact)
                .check("the center anchor is a pane of its own again", ctx -> {
                    var editor = editor(ctx);
                    return editor.centerWindow != editor.rootWindow
                            && Editor.ANCHOR_CENTER.equals(editor.centerWindow.getAnchorId())
                            && contains(editor.rootWindow, editor.centerWindow);
                })

                .step("apply a layout naming the right pane twice",
                        ctx -> editor(ctx).applyLayout(layout(Editor.ANCHOR_RIGHT, Editor.ANCHOR_RIGHT)))
                .frames(2)
                .check("the window tree is intact", EditorLayoutAnchorScenario::intact)
                .check("one window carries the right anchor", ctx -> carrying(editor(ctx).rootWindow, Editor.ANCHOR_RIGHT) == 1)

                .step("apply a layout naming the root below itself", ctx -> editor(ctx).applyLayout(new EditorLayout(
                        new LayoutConfig(Editor.ANCHOR_ROOT, false, 80,
                                new LayoutConfig(Editor.ANCHOR_ROOT, false, 50, null, null),
                                new LayoutConfig(Editor.ANCHOR_RIGHT, false, 50, null, null)), List.of())))
                .frames(2)
                .check("the window tree is intact", EditorLayoutAnchorScenario::intact)
                .check("one window carries the root anchor", ctx -> carrying(editor(ctx).rootWindow, Editor.ANCHOR_ROOT) == 1)

                .step("dock views into two unnamed panes", ctx -> {
                    var names = viewNames(ctx);
                    var panes = new LayoutConfig(null, true, 50,
                            new LayoutConfig(null, false, 50, null, null), new LayoutConfig(null, false, 50, null, null));
                    var right = new LayoutConfig(Editor.ANCHOR_RIGHT, false, 50, null, null);
                    editor(ctx).applyLayout(new EditorLayout(new LayoutConfig(Editor.ANCHOR_ROOT, false, 80, panes, right), List.of(
                            new EditorLayout.SlotEntry("ff", List.of(names.get(0)), null),
                            new EditorLayout.SlotEntry("fs", List.of(names.get(1)), null))));
                })
                .frames(2)
                .check("the window tree is intact", EditorLayoutAnchorScenario::intact)
                .step("move every view out of them", ctx -> editor(ctx).applyLayout(new EditorLayout(
                        layout(Editor.ANCHOR_CENTER, Editor.ANCHOR_RIGHT).layoutConfig(),
                        List.of(new EditorLayout.SlotEntry("s", viewNames(ctx), null)))))
                .frames(2)
                .check("the window tree is intact", EditorLayoutAnchorScenario::intact)
                .screenshot("01_after_layouts")
                .closeScreen();
    }

    /** root | right, the part left of right split into a center pane over the bottom one. */
    private static EditorLayout layout(@Nullable String centerAnchor, String rightAnchor) {
        var center = new LayoutConfig(centerAnchor, false, 50, null, null);
        var bottom = new LayoutConfig(Editor.ANCHOR_BOTTOM, false, 50, null, null);
        var main = new LayoutConfig(null, true, 75, center, bottom);
        var right = new LayoutConfig(rightAnchor, false, 50, null, null);
        return new EditorLayout(new LayoutConfig(Editor.ANCHOR_ROOT, false, 80, main, right), List.of());
    }

    /** Every window sits once in the tree, its parent pointing back at it, and no docked view went missing. */
    private static boolean intact(TestContext ctx) {
        var root = editor(ctx).rootWindow;
        var structure = new StringBuilder();
        var problems = new StringBuilder();
        describe(root, "r", Collections.newSetFromMap(new IdentityHashMap<>()), structure, problems);
        int views = root.getAllViews().size();
        if (views != ctx.<Integer>get(VIEWS)) {
            problems.append(" views ").append(ctx.<Integer>get(VIEWS)).append("->").append(views).append(';');
        }
        if (!problems.isEmpty()) {
            throw new AssertionError("window tree:" + problems + " |" + structure);
        }
        return true;
    }

    private static void describe(SplittableWindow window, String path, Set<SplittableWindow> seen,
                                 StringBuilder structure, StringBuilder problems) {
        if (!seen.add(window)) {
            problems.append(' ').append(path).append(" seen twice;");
            return;
        }
        structure.append(' ').append(path).append('[').append(window.getAnchorId())
                .append(window.getViewContainer() != null ? " leaf " + window.getViewContainer().getAllViews().size() : "").append(']');
        if (window.getViewContainer() == null && (window.getSplitView() == null || window.getFirst() == null || window.getSecond() == null)) {
            problems.append(' ').append(path).append(" is a split missing a side;");
        }
        var children = new SplittableWindow[]{window.getFirst(), window.getSecond()};
        for (int i = 0; i < 2; i++) {
            var child = children[i];
            if (child == null) continue;
            var childPath = path + (i == 0 ? "f" : "s");
            if (child.getParentWindow() != window) {
                problems.append(' ').append(childPath).append(" parent mismatch;");
            }
            describe(child, childPath, seen, structure, problems);
        }
    }

    private static List<String> viewNames(TestContext ctx) {
        return editor(ctx).rootWindow.getAllViews().stream().map(view -> view.getName()).toList();
    }

    private static int carrying(@Nullable SplittableWindow window, String anchorId) {
        if (window == null) return 0;
        return (anchorId.equals(window.getAnchorId()) ? 1 : 0)
                + carrying(window.getFirst(), anchorId) + carrying(window.getSecond(), anchorId);
    }

    private static boolean contains(SplittableWindow window, SplittableWindow target) {
        if (window == target) return true;
        return window.getFirst() != null && contains(window.getFirst(), target)
                || window.getSecond() != null && contains(window.getSecond(), target);
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }
}
