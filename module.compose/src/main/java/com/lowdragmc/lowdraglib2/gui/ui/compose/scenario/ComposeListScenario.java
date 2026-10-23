package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestCompose;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.target.Texts;

import java.util.ArrayList;
import java.util.List;

/**
 * End-to-end check that Compose state changes reach the real element tree through insert, remove and
 * move, the move keeping element identity. Doubles as the load-time check for the jar-in-jar'd runtime.
 */
@LDLRegisterClient(name = "compose_list", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeListScenario implements UIScenario {

    private final List<UIElement> beforeReverse = new ArrayList<>();

    /** The styled text's computed values before anything highlights it. */
    private float baseFontSize;
    private int baseTextColor;


    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("compose").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("compose demo screen", ctx -> new TestCompose().createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement(".add-button")

                .checkCount(".item-button", 3)
                .check("starts as A, B, C", ctx -> captions(ctx).equals(List.of("A", "B", "C")))

                // insert
                .click(".add-button")
                // Frames, not ticks: recomposition runs off the frame callback.
                .frames(3)
                .click(".add-button")
                .frames(3)
                .checkCount(".item-button", 5)
                .check("appends in order", ctx ->
                        captions(ctx).equals(List.of("A", "B", "C", "Button 4", "Button 5")))
                .screenshotElement("compose-list-grown", ".__root__")

                // move
                .step("remember which elements are on screen", ctx -> {
                    beforeReverse.clear();
                    beforeReverse.addAll(elements(ctx));
                })
                .click(".reverse-button")
                .frames(3)
                .check("reversed the captions", ctx ->
                        captions(ctx).equals(List.of("Button 5", "Button 4", "C", "B", "A")))
                .check("reused the same elements rather than rebuilding them", ctx -> {
                    var after = elements(ctx);
                    if (after.size() != beforeReverse.size()) return false;
                    for (int i = 0; i < after.size(); i++) {
                        if (after.get(i) != beforeReverse.get(beforeReverse.size() - 1 - i)) return false;
                    }
                    return true;
                })

                // remove
                .click(".reset-button")
                .frames(3)
                .checkCount(".item-button", 3)
                .check("back to A, B, C", ctx -> captions(ctx).equals(List.of("A", "B", "C")))
                .screenshotElement("compose-list-reset", ".__root__")

                // Highlight on and off: the class is diffed away, while the style block is never undone,
                // so it is the demo's un-highlighted branch that writes the sampled values back.
                .check("starts unhighlighted", ctx -> !hasHotClass(ctx))
                .step("sample the unstyled values", ctx -> {
                    baseFontSize = fontSize(ctx);
                    baseTextColor = textColor(ctx);
                })

                .click(".highlight-toggle")
                .frames(3)
                .check("highlight adds the class", ComposeListScenario::hasHotClass)
                .check("highlight sets the font size", ctx -> fontSize(ctx) == 18f)
                .check("highlight sets the text colour", ctx -> textColor(ctx) == 0xFFFF5555)
                .check("the highlight actually changed something", ctx ->
                        baseFontSize != 18f && baseTextColor != 0xFFFF5555)

                .click(".highlight-toggle")
                .frames(3)
                .check("un-highlight removes the class", ctx -> !hasHotClass(ctx))
                .check("un-highlight restores the font size", ctx -> fontSize(ctx) == baseFontSize)
                .check("un-highlight restores the text colour", ctx -> textColor(ctx) == baseTextColor)

                .closeScreen();
    }

    /**
     * Walks the tree rather than {@code ctx.query()}, which lists elements in registration order and
     * so still shows the old order after a move.
     */
    private static List<UIElement> elements(TestContext ctx) {
        var scroller = ctx.query().type(ScrollerView.class).one().as(ScrollerView.class);
        var buttons = new ArrayList<UIElement>();
        for (var row : scroller.getContentHost().getChildren()) {
            for (var child : row.getChildren()) {
                if (child.hasClass("item-button")) buttons.add(child);
            }
        }
        return buttons;
    }

    private static List<String> captions(TestContext ctx) {
        return elements(ctx).stream().map(Texts::of).toList();
    }

    private static boolean hasHotClass(TestContext ctx) {
        return ctx.query().select(".styled-text").one().hasClass("hot");
    }

    private static TextElement styledText(TestContext ctx) {
        return ctx.query().select(".styled-text").one().as(TextElement.class);
    }

    /** Reads back through the style facade, i.e. the value the renderer will actually use. */
    private static float fontSize(TestContext ctx) {
        return styledText(ctx).getTextStyle().fontSize();
    }

    private static int textColor(TestContext ctx) {
        return styledText(ctx).getTextStyle().textColor();
    }
}
