package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorLayout;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

/**
 * Focus mode on an editor pane: double-click a tab and that pane fills the editor, double-click
 * again and every splitter is exactly where it was.
 *
 * <p>The interesting assertion is not that the pane grows — it is that
 * {@link Editor#captureLayout()} still reports the <em>real</em> splitter percentages while a pane is
 * maximized. Maximizing works by overriding the on-path splitter to 100% at IMPORTANT priority, and
 * that is precisely the value {@code SplitView#getPercentage} reads back, so a layout saved during a
 * maximize would have pinned every pane to all-or-nothing. Comparing the captured layout against the
 * baseline is what proves the auto-restore in {@code captureLayout} is doing its job.
 */
@LDLRegisterClient(name = "editor_pane_maximize", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class EditorPaneMaximizeScenario implements UIScenario {

    private static final String BASELINE = "baseline_layout";
    private static final String DOCKED_WIDTH = "docked_center_width";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "dock", "maximize").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)

                .step("record the baseline layout and give the centre tab an id", ctx -> {
                    var editor = editor(ctx);
                    ctx.put(BASELINE, editor.captureLayout());
                    ctx.put(DOCKED_WIDTH, editor.centerWindow.getSizeWidth());
                    var container = editor.centerWindow.getViewContainer();
                    ctx.require("the centre pane has a view to click",
                            container != null && !container.getAllViews().isEmpty());
                    var tab = container.tabView.getTabContents().inverse().get(container.getAllViews().get(0));
                    ctx.require("the view has a tab", tab != null);
                    // Tabs carry no stable id of their own, and the selector engine has no way to
                    // reach one by the view it hosts. Naming it here beats guessing at a path.
                    tab.setId("centre_tab");
                })
                .check("nothing is maximized to begin with",
                        ctx -> editor(ctx).rootWindow.getMaximizedWindow() == null)
                .screenshot("01_docked")

                .group("double click a tab to maximize its pane", g -> g
                        .hover("#centre_tab")
                        .step("double click the tab", EditorPaneMaximizeScenario::doubleClickCentreTab)
                        .settleMs(120)
                        .check("the centre pane reports maximized",
                                ctx -> editor(ctx).centerWindow.isMaximized())
                        .check("the right pane is hidden", ctx -> !rightPaneSlot(ctx).isDisplayed())
                        .check("the centre pane actually grew",
                                ctx -> editor(ctx).centerWindow.getSizeWidth()
                                        > ctx.<Float>get(DOCKED_WIDTH) * 1.5f)
                        .screenshot("02_maximized"))

                .group("capturing a layout while maximized reports the real percentages", g -> g
                        .check("captureLayout matches the baseline",
                                ctx -> editor(ctx).captureLayout().equals(ctx.<EditorLayout>get(BASELINE)))
                        .check("capturing dropped the focus mode",
                                ctx -> editor(ctx).rootWindow.getMaximizedWindow() == null))

                .group("double click again to restore", g -> g
                        .step("maximize once more", ctx -> editor(ctx).centerWindow.maximize())
                        .settleMs(120)
                        .hover("#centre_tab")
                        .step("double click the tab", EditorPaneMaximizeScenario::doubleClickCentreTab)
                        .settleMs(120)
                        .check("nothing is maximized", ctx -> editor(ctx).rootWindow.getMaximizedWindow() == null)
                        .check("the right pane is visible again", ctx -> rightPaneSlot(ctx).isDisplayed())
                        .check("the centre pane is back to its docked width",
                                ctx -> Math.abs(editor(ctx).centerWindow.getSizeWidth()
                                        - ctx.<Float>get(DOCKED_WIDTH)) < 1f)
                        .check("the layout is identical to the baseline",
                                ctx -> editor(ctx).captureLayout().equals(ctx.<EditorLayout>get(BASELINE)))
                        .screenshot("03_restored"))

                // Press and release in one step rather than through rightClick(selector). The menu
                // opens on MOUSE_DOWN and is positioned at the cursor, so by the time the builder's
                // separate release step re-resolves the selector the tab is behind the menu and its
                // occlusion guard — rightly — refuses to click it.
                .group("right click a tab opens its menu", g -> g
                        .hover("#centre_tab")
                        .step("press and release the right button on the tab", ctx -> {
                            var bounds = ctx.el("#centre_tab").bounds();
                            ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 1);
                            ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 1);
                        })
                        .settleMs(120)
                        .check("a menu is open", ctx -> ctx.count("menu") > 0)
                        .check("the menu has entries",
                                ctx -> !ctx.query("menu").nth(0).one().element().getChildren().isEmpty())
                        .screenshot("04_tab_menu"))

                .closeScreen();
    }

    /**
     * Both clicks in a single step.
     *
     * <p>{@code ScenarioBuilder#doubleClick} spreads its two clicks over several frames, and
     * {@code ModularUI} only pairs them when they land within 300ms of each other — so on a busy
     * machine the gesture silently degrades into two single clicks and the test fails for a reason
     * that has nothing to do with what it is testing.
     */
    private static void doubleClickCentreTab(TestContext ctx) {
        var bounds = ctx.el("#centre_tab").bounds();
        for (int i = 0; i < 2; i++) {
            ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), 0);
            ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), 0);
        }
    }

    static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    /**
     * The root split's second slot, which holds the right-hand pane — the one a maximize of the
     * centre pane has to hide on its way up to the root.
     */
    private static UIElement rightPaneSlot(TestContext ctx) {
        var splitView = editor(ctx).rootWindow.getSplitView();
        if (splitView == null) throw new IllegalStateException("Root window is not split");
        return splitView.second;
    }
}
