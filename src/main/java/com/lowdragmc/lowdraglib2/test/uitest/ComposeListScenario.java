package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.ui.TestCompose;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.target.Texts;

import java.util.ArrayList;
import java.util.List;

/**
 * End-to-end check that Compose state changes reach the real element tree.
 *
 * <p>Nothing in {@link TestCompose} tells the UI to rebuild — a click only changes a list held in
 * `remember`. So a passing run means the whole chain worked: the snapshot write was published, the
 * recomposer picked it up off the frame callback, and the applier edited a live {@code ModularUI}.
 *
 * <p>All three applier operations are covered, because they fail in different ways: insert is the
 * common path, remove has to pick the right children out of a list that also holds the widgets'
 * internal parts, and move has to reorder without detaching. The move case checks element
 * <em>identity</em> rather than just order — reordering by remove-and-re-add would produce the right
 * order too, while silently dropping focus and restarting animations.
 *
 * <p>It doubles as the load-time check for the jar-in-jar'd Compose runtime and coroutines: if those
 * did not resolve under NeoForge's module layer, opening the screen would throw rather than fail an
 * assertion.
 */
@LDLRegisterClient(name = "compose_list", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeListScenario implements UIScenario {

    /** Captured before the reverse, compared after it. */
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
                // Frames, not ticks: recomposition runs off the frame callback, so a rendered frame
                // is all it takes for the new row to exist.
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
                        // Reversed, so position i now holds what was at size-1-i - the same instance.
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

                // Modifier diff, both directions. Everything else in this demo has a fixed chain per
                // call site, so ops only ever get applied; here dropping one has to undo it — and
                // undoing a style means the element goes back to what the stylesheet said, not to
                // some value the modifier wrote over the top.
                .check("starts unhighlighted", ctx -> !hasHotClass(ctx))
                // Sampled rather than hardcoded: the baseline is whatever the stylesheet computed,
                // which is the value un-applying has to get back to - not the property's default.
                .step("sample the unstyled values", ctx -> {
                    baseFontSize = fontSize(ctx);
                    baseTextColor = textColor(ctx);
                })

                .click(".highlight-toggle")
                .frames(3)
                .check("highlight adds the class", ComposeListScenario::hasHotClass)
                // Typed sugar and the generic style() escape hatch, respectively.
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
     * Walks the tree rather than going through {@code ctx.query()}.
     *
     * <p>{@link com.lowdragmc.lowdraglib2.gui.ui.ModularUI#select} filters
     * {@code getAllElements()}, which is in element <em>registration</em> order. Reordering siblings
     * does not re-register anything, so after a move the query still lists the old order even though
     * the tree and the rendered UI are both correct. Order-sensitive assertions have to read the
     * child lists directly.
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
