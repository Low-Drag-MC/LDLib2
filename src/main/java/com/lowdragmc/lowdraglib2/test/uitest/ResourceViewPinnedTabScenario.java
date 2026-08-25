package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceView;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.test.TestProject;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

/**
 * The asset browser's tab stays put at the top of the resource view while the per resource type tabs
 * scroll underneath it.
 *
 * <p>It used to be the first child of the same scroller as everything else, so a project with enough
 * resources to overflow the strip could push the one way into the file system out of sight. What makes
 * it un-scrollable now is simply that its header is not in the scroller at all — which is the thing
 * worth asserting, since a position check alone would pass just as well on a strip too short to
 * scroll.
 */
@LDLRegisterClient(name = "resource_view_pinned_tab", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ResourceViewPinnedTabScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "resources", "layout");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("show the resource view and load a project that fills it with tabs", ctx -> {
                    var view = resourceView(ctx);
                    var container = view.getViewContainer();
                    ctx.require("the resource view is docked", container != null);
                    container.selectView(view);
                    editor(ctx).loadProject(new TestProject(), null);
                })
                .waitUntil("the resource tabs are up", ctx -> !resourceView(ctx).getResourceTabs().isEmpty())

                .group("the browser tab is out of the scroller and above it", g -> g
                        .check("its header is not a scroller child", ctx -> {
                            var view = resourceView(ctx);
                            return !view.tabView.tabScroller.hasScrollViewChild(view.getAssetBrowserTab());
                        })
                        .check("it is the first thing in the header", ctx -> {
                            var view = resourceView(ctx);
                            return view.tabView.tabHeaderContainer.getChildren().getFirst()
                                    == view.getAssetBrowserTab();
                        })
                        .check("the divider sits right after it", ctx -> {
                            var view = resourceView(ctx);
                            return view.tabView.tabHeaderContainer.getChildren().get(1)
                                    == view.pinnedTabSeparator;
                        })
                        // A percentage width inside an auto-width column is exactly the kind of thing
                        // that silently resolves to zero, and a divider nobody can see is not a divider.
                        .check("the divider has a visible size", ctx -> {
                            var separator = resourceView(ctx).pinnedTabSeparator;
                            return separator.getSizeWidth() > 0 && separator.getSizeHeight() > 0;
                        })
                        .check("it sits above the scrolling strip", ctx -> {
                            var view = resourceView(ctx);
                            var tab = view.getAssetBrowserTab();
                            return tab.getPositionY() + tab.getSizeHeight()
                                    <= view.tabView.tabScroller.getPositionY() + 0.5f;
                        })
                        .check("every resource tab is still in the scroller", ctx -> {
                            var view = resourceView(ctx);
                            return view.tabView.tabScroller.viewContainer.getChildren().size()
                                    == view.getResourceTabs().size();
                        })
                        .step("name the header strip so it can be captured on its own", ctx ->
                                resourceView(ctx).tabView.tabHeaderContainer.setId("resource_tab_header"))
                        .screenshot("01_pinned")
                        // The whole editor at once is too small to tell a one pixel divider from a gap.
                        .screenshotElement("02_header_strip", "#resource_tab_header"))

                .group("selection is unchanged", g -> g
                        .step("name the tabs so they can be clicked", ctx -> {
                            var view = resourceView(ctx);
                            view.getAssetBrowserTab().setId("pinned_tab");
                            var resourceTab = view.getResourceTabs().values().stream().findFirst().orElse(null);
                            ctx.require("there is a resource tab to click", resourceTab != null);
                            resourceTab.setId("resource_tab");
                        })
                        .click("#resource_tab")
                        .check("the resource tab took the selection", ctx -> {
                            var view = resourceView(ctx);
                            return view.tabView.getSelectedTab() == view.getResourceTabs().values()
                                    .stream().findFirst().orElse(null)
                                    && view.getSelectedResourceInstance() != null;
                        })
                        .screenshot("02_resource_selected")
                        .click("#pinned_tab")
                        .check("the pinned tab takes it back", ctx -> {
                            var view = resourceView(ctx);
                            return view.tabView.getSelectedTab() == view.getAssetBrowserTab()
                                    // the browser is not tied to a resource type, so nothing stays selected
                                    && view.getSelectedResourceInstance() == null;
                        })
                        .check("the browser is the visible tab content",
                                ctx -> resourceView(ctx).getAssetBrowser().isDisplayed())
                        .screenshot("03_browser_selected"))

                .closeScreen();
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    private static ResourceView resourceView(TestContext ctx) {
        return editor(ctx).resourceView;
    }
}
