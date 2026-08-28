package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestComposeWidgets;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

/**
 * Covers the two claims the other compose scenarios leave untested.
 *
 * <p><b>Subcomposition is invisible.</b> A split view's panes are separate compositions wired to the
 * parent through a {@code CompositionContext}. If that wiring is wrong the symptoms are subtle
 * rather than loud: the panes render, but state changes stop crossing the boundary, or a
 * {@code CompositionLocal} provided outside reads as its default inside. Both are checked.
 *
 * <p><b>Reordering keeps focus.</b> {@code UIElement.reorderChild} exists so a keyed list can shuffle
 * without detaching anything. Remove-and-re-add would give the same visible order, so order alone
 * proves nothing — the assertion that has teeth is that the focused field is still focused, and is
 * still the same element, after the swap.
 */
@LDLRegisterClient(name = "compose_widgets", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeWidgetsScenario implements UIScenario {

    private UIElement fieldABefore;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("compose").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("compose widgets demo", ctx -> new TestComposeWidgets().createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement(".demo-split")

                // Both panes live in their own composition, and both read the same state and the
                // same CompositionLocal from outside the split.
                .checkText(".pane-left", "left provided 0")
                .checkText(".pane-right", "right provided 0")

                .click(".pane-button-left")
                .frames(3)
                .check("a change in one pane reaches the other", ctx ->
                        text(ctx, ".pane-left").equals("left provided 1")
                                && text(ctx, ".pane-right").equals("right provided 1"))

                .click(".pane-button-right")
                .click(".pane-button-right")
                .frames(3)
                .check("both panes stay in step", ctx ->
                        text(ctx, ".pane-left").equals("left provided -1")
                                && text(ctx, ".pane-right").equals("right provided -1"))
                .screenshotElement("widgets-split", ".__root__")

                // Focus survival across a reorder.
                .click(".field-a")
                .frames(2)
                .checkFocused(".field-a")
                .step("remember which element field-a is", ctx -> fieldABefore = element(ctx, ".field-a"))

                // Dispatched rather than clicked. A real click would move focus to the button first
                // — correct behaviour, but it would mean this test measured the input system's focus
                // policy instead of what the reorder does to focus.
                .step("swap without touching focus", ctx -> press(ctx, ".swap-fields"))
                .frames(3)
                .check("the fields swapped places", ctx -> {
                    var row = element(ctx, ".field-a").getParent();
                    return row != null && row.getChildren().get(0).hasClass("field-b");
                })
                .check("field-a is the same element it was", ctx -> element(ctx, ".field-a") == fieldABefore)
                // The one that would fail if `move` were implemented as remove plus re-add.
                .checkFocused(".field-a")

                // AnimatedVisibility against a real element tree. Frames, not ticks: the tween runs
                // off the frame clock, and a dev world renders far faster than it ticks.
                .checkNotExists(".fade-content")
                // Pressed rather than clicked: a full click settles between hover, press and
                // release, and those settles alone outlast the fade — leaving nothing to observe.
                .step("show the panel", ctx -> press(ctx, ".fade-toggle"))
                .frames(3)
                .checkExists(".fade-content")
                .waitUntil("the fade-in reaches full opacity", ctx -> opacity(ctx) >= 1f)

                .step("hide the panel", ctx -> press(ctx, ".fade-toggle"))
                .frames(2)
                // Mid-fade the element is still mounted, and already partly transparent. A plain
                // `if (visible)` would have removed it on the first frame.
                .check("still present while fading out", ctx ->
                        ctx.query().select(".fade-content").optional().isPresent())
                .check("and already fading", ctx -> opacity(ctx) < 1f)
                .waitUntil("the fade-out finishes and unmounts it", ctx ->
                        ctx.query().select(".fade-content").optional().isEmpty())

                // Tabs. Only the selected tab's body is displayed, but every tab stays composed —
                // which is what lets a tab keep its state while you are looking at another one.
                .check("tab one is showing", ctx -> paneShown(ctx, ".tab-body-one"))
                .check("tab two is composed but not showing", ctx -> !paneShown(ctx, ".tab-body-two"))
                .click(".tab-select-two")
                .frames(3)
                .check("selection moved to tab two", ctx ->
                        paneShown(ctx, ".tab-body-two") && !paneShown(ctx, ".tab-body-one"))

                // Adding and removing a tab, which is the case that leaks if teardown is missing.
                .checkNotExists(".tab-body-three")
                .click(".tab-toggle")
                .frames(3)
                .checkExists(".tab-body-three")
                .click(".tab-toggle")
                .frames(3)
                .checkNotExists(".tab-body-three")
                .check("removing a tab took its header with it", ctx ->
                        ctx.query().type(Tab.class).count() == 2)
                .screenshotElement("widgets-tabs", ".__root__")

                // A modal driven by state. Its content has to be composed before `show()` runs, or
                // the dialog decides it has none and displays an empty body.
                .checkNotExists(".dialog-body")
                // Pressed, not clicked: the dialog opens on mouse-down and its modal overlay then
                // covers the button, so re-resolving the selector for the release fails.
                .step("open the dialog", ctx -> press(ctx, ".dialog-open"))
                .frames(3)
                .checkExists(".dialog-body")
                .checkVisible(".dialog-body")

                // Closing from inside: the dialog's own button writes the same state `visible` reads.
                .step("confirm", ctx -> press(ctx, ".dialog-confirm"))
                .frames(3)
                .checkNotExists(".dialog-body")
                .checkTextContains(".dialog-answer", "yes")

                // A widget with no wrapper of its own, used through the generic escape hatch. It
                // mounts, and its properties track composition state, with no per-widget code.
                .check("the unwrapped widget mounted", ctx ->
                        ctx.query().type(TreeList.class).count() == 1)
                .check("it shows the two branches it was given", ctx -> treeRows(ctx) == 2)
                .click(".tree-grow")
                .frames(3)
                .check("changing state re-drives it", ctx -> treeRows(ctx) == 4)

                .closeScreen();
    }

    private static UIElement element(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().as(UIElement.class);
    }

    /** Fires a left mouse-down straight at an element, which is what a Button acts on. */
    private static void press(TestContext ctx, String selector) {
        var event = UIEvent.create(UIEvents.MOUSE_DOWN);
        event.target = element(ctx, selector);
        event.button = 0;
        UIEventDispatcher.dispatchEvent(event);
    }

    private static String text(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().text();
    }

    /** How many rows the unwrapped tree widget is showing. */
    private static int treeRows(TestContext ctx) {
        var tree = ctx.query().type(TreeList.class).one().as(TreeList.class);
        return tree.getChildren().size();
    }

    /** The fade panel's current opacity, read back through the style facade. */
    private static float opacity(TestContext ctx) {
        return element(ctx, ".fade-panel").getStyle().opacity();
    }

    /**
     * Whether the pane holding {@code bodySelector} is the one on show.
     *
     * <p>Asks the pane, not the body. {@code isDisplayed()} reports an element's own display flag and
     * does not consult its ancestors, and it is the pane that a tab view shows and hides — so the
     * body inside a hidden pane still answers "displayed".
     */
    private static boolean paneShown(TestContext ctx, String bodySelector) {
        var pane = element(ctx, bodySelector).getParent();
        return pane != null && pane.isDisplayed();
    }
}
