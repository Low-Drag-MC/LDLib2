package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

/** Captures the canonical component examples for the Wiki component reference pages. */
@LDLRegisterClient(name = "wiki_component_gallery", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class WikiComponentGalleryScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(100).tags("wiki", "components", "visual").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openScreenTest("component_examples")
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#right_container");

        capture(s, "button");
        s.screenshot("template-layout");
        capture(s, "label");
        capture(s, "text-area");
        capture(s, "progress-bar");
        capture(s, "item-fluid-slots");
        capture(s, "toggle");
        capture(s, "switch");
        capture(s, "selector");
        capture(s, "scroller");
        capture(s, "text-field");
        capture(s, "tag-field");
        capture(s, "structured-tag-editor");
        capture(s, "tree-list");
        capture(s, "code-editor");
        capture(s, "scene");
        capture(s, "search-component");
        capture(s, "color-selector");
        capture(s, "tab-view");
        capture(s, "scroller-view");
        capture(s, "graph-view");
        capture(s, "split-view");

        s.closeScreen();
    }

    private static void capture(ScenarioBuilder s, String slug) {
        s.step("show " + slug, ctx -> ctx.el("#example-" + slug)
                .as(Toggle.class).setValue(true, true));
        if (slug.equals("selector")) {
            s.step("open direction selector", ctx -> ctx.query().type(Selector.class).list().getFirst()
                    .as(Selector.class).show());
        } else if (slug.equals("search-component")) {
            s.step("open search component", ctx -> ctx.query().type(SearchComponent.class).one()
                    .as(SearchComponent.class).show())
                    .typeInto("text-field", "stone")
                    .ticks(3);
        }
        s.frames(2).screenshotElement(slug, "#right_container");
    }
}
