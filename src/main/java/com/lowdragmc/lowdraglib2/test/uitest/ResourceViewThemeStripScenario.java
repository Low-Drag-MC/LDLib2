package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceTabLayout;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceTabLayoutStore;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceView;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.test.TestProject;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The resource view's tab strip in every built-in theme, on both sides it can sit on, as pictures.
 *
 * <p>The strip and the resource pane meet along a seam that each theme draws as one rounded shape cut
 * in two — the strip rounds the outer corners, the pane rounds the ones facing away from it. Which
 * corners those are depends on which side the strip is on, so every theme carries a second set of
 * rules for the strip on top, and a theme that forgot them does not look subtly wrong: the rounding
 * ends up on the wrong edges and the seam stops lining up.
 *
 * <p>There is no boolean for that, so this scenario exists to produce the evidence. It does check the
 * one thing that breaks silently — that each theme still parses into a sheet with rules in it, since
 * a typo in a selector is logged and dropped rather than raised.
 */
@LDLRegisterClient(name = "resource_view_theme_strip", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ResourceViewThemeStripScenario implements UIScenario {

    private static final List<String> THEMES =
            List.of("mc", "modern", "ore", "dusk", "carbon", "mint", "plum", "paper", "latte");
    /** Thick enough for two tabs to a line in every theme, so the wrapped case is in every shot. */
    private static final float STRIP_SIZE = 46;
    private static final int MIN_RULES = 20;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "resources", "theme", "visual");
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
                    // Named so the strip and the seam can be captured together, which is the whole
                    // point: the two halves of the rounding only read as wrong side by side.
                    view.setId("resource_view");
                    editor(ctx).loadProject(new TestProject(), null);
                })
                .waitUntil("the resource tabs are up", ctx -> !resourceView(ctx).getResourceTabs().isEmpty())
                .step("start from a strip nobody has touched", ctx -> resourceView(ctx).resetTabLayout())
                // Off the strip, so a tab's tooltip is not sitting over the seam in every shot.
                .step("park the pointer away from the tabs", ctx -> {
                    var editor = editor(ctx);
                    ctx.input().moveTo(editor.getPositionX() + editor.getSizeWidth() / 2f,
                            editor.getPositionY() + 4);
                })
                .frames(2);

        for (var theme : THEMES) {
            s.step("switch to " + theme, ctx -> applyTheme(ctx, theme))
                    .check(theme + " loaded with its rules", ctx -> {
                        var rules = StylesheetManager.INSTANCE.getStylesheetSafe(location(theme)).rules.size();
                        ctx.log("%s: %d rules".formatted(theme, rules));
                        return rules >= MIN_RULES;
                    });
            var index = new int[]{1};
            for (var placement : ResourceTabLayout.Placement.values()) {
                var side = placement.styleName();
                s.step("%s: put the tabs on the %s, two to a line".formatted(theme, side), ctx -> {
                    var view = resourceView(ctx);
                    view.setPlacement(placement);
                    view.setStripSize(STRIP_SIZE);
                })
                        .frames(3)
                        // The one thing a check cannot judge is whether the seam looks like one shape
                        // cut in two, so every theme is shot on every side it can be put on. What can
                        // be checked is that the theme left its tabs room, which is the same invariant
                        // resource_view_tab_strip holds the layout to.
                        .step("%s: the tabs fit the %s strip".formatted(theme, side), ctx ->
                                ResourceViewTabStripScenario.checkTabsFit(ctx,
                                        "%s on the %s: a tab fits the strip".formatted(theme, side)))
                        .screenshotElement("%s_%02d_%s".formatted(theme, index[0]++, side), "#resource_view");
            }
        }

        s.closeScreen()
                // The arrangement is saved per editor and outlives the run; straight at the store
                // because a teardown also runs after a failure, when there may be no screen left.
                .teardown("leave the saved strip as it was found", ctx ->
                        ResourceTabLayoutStore.save(TestEditor.class.getName(), new ResourceTabLayout()));
    }

    private static void applyTheme(TestContext ctx, String theme) {
        var engine = ctx.requireUI().getStyleEngine();
        engine.clearAllStylesheets();
        engine.addStylesheet(StylesheetManager.INSTANCE.getStylesheetSafe(location(theme)));
    }

    private static Identifier location(String theme) {
        return LDLib2.id(StylesheetManager.PATH + "/" + theme + ".lss");
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    private static ResourceView resourceView(TestContext ctx) {
        return editor(ctx).resourceView;
    }
}
