package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

import java.util.List;

/**
 * Every built-in theme, over the same components, as pictures.
 *
 * <p>A stylesheet is the one kind of change whose result cannot be asserted — "does this look good"
 * has no boolean — so this scenario exists to <em>produce the evidence</em> rather than to pass. It
 * drives the component gallery once per theme and captures the same set of components each time, which
 * makes the themes comparable side by side and makes a theme that forgot a component obvious: an
 * unstyled control does not look subtly wrong, it looks like a different UI.
 *
 * <p>It does check one thing, and it is the thing that silently breaks: that every theme still parses
 * into a stylesheet with rules in it. A typo in a selector is logged and dropped, so a broken sheet
 * loads as a smaller sheet rather than as an error.
 */
@LDLRegisterClient(name = "theme_gallery", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ThemeGalleryScenario implements UIScenario {

    /** The themes to shoot, and how many rules each must at least have to count as loaded. */
    private static final List<String> THEMES =
            List.of("dusk", "carbon", "mint", "plum", "paper", "latte");
    private static final int MIN_RULES = 40;

    /**
     * The components worth a picture: between them they cover every surface, control, input and state
     * a theme sets. Deliberately not all of them — the gallery has twenty-one, and the rest are built
     * out of these.
     */
    private static final List<String> COMPONENTS = List.of(
            "button", "toggle", "switch", "text-field", "selector",
            "progress-bar", "tab-view", "color-selector", "tree-list", "item-fluid-slots");

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("theme", "visual", "components").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openScreenTest("component_examples")
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#right_container");

        for (var theme : THEMES) {
            s.step("switch to " + theme, ctx -> applyTheme(ctx, theme))
                    // the same component in the pane for every theme's overview shot, so the four
                    // pictures differ by theme and by nothing else
                    .step("%s: settle on a known component".formatted(theme),
                            ctx -> ctx.el("#example-button").as(Toggle.class).setValue(true, true))
                    .frames(3)
                    .check(theme + " loaded with its rules", ctx -> {
                        var sheet = StylesheetManager.INSTANCE.getStylesheetSafe(location(theme));
                        var rules = sheet.rules.size();
                        ctx.log("%s: %d rules".formatted(theme, rules));
                        return rules >= MIN_RULES;
                    })
                    .screenshot(theme + "_00_layout");
            for (var component : COMPONENTS) {
                capture(s, theme, component);
            }
        }

        s.closeScreen();
    }

    private static void capture(ScenarioBuilder s, String theme, String component) {
        s.step("%s: show %s".formatted(theme, component),
                ctx -> ctx.el("#example-" + component).as(Toggle.class).setValue(true, true));
        if (component.equals("selector")) {
            // open it, so the dialog and its rows are in the shot rather than just the closed control
            s.step("%s: open the selector".formatted(theme), ctx -> ctx.query().type(Selector.class)
                    .list().getFirst().as(Selector.class).show());
        }
        s.frames(2).screenshotElement("%s_%s".formatted(theme, component), "#right_container");
        if (component.equals("selector")) {
            // and close it again: an overlay left open floats over every shot that follows
            s.step("%s: close the selector".formatted(theme), ctx -> ctx.query().type(Selector.class)
                    .list().getFirst().as(Selector.class).hide());
        }
    }

    private static void applyTheme(TestContext ctx, String theme) {
        var engine = ctx.requireUI().getStyleEngine();
        engine.clearAllStylesheets();
        engine.addStylesheet(StylesheetManager.INSTANCE.getStylesheetSafe(location(theme)));
    }

    private static net.minecraft.resources.ResourceLocation location(String theme) {
        return LDLib2.id(StylesheetManager.PATH + "/" + theme + ".lss");
    }
}
