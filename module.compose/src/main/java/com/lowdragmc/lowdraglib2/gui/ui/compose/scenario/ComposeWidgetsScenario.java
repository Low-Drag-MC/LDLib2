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
import com.lowdragmc.lowdraglib2.uitest.target.Texts;

import java.util.List;

/**
 * The widgets demo: split-view panes sharing state and a {@code CompositionLocal} across their
 * composition boundary, a keyed reorder keeping focus, and the visibility, tab, dialog, escape-hatch,
 * hover, remount and node-observer cases.
 */
@LDLRegisterClient(name = "compose_widgets", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeWidgetsScenario implements UIScenario {

    private UIElement fieldABefore;
    private UIElement tabTwoBefore;
    private float belowY;

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

                // Dispatched rather than clicked: a click would move focus to the button and test the
                // input system's focus policy instead of the reorder.
                .step("swap without touching focus", ctx -> press(ctx, ".swap-fields"))
                .frames(3)
                .check("the fields swapped places", ctx -> {
                    var row = element(ctx, ".field-a").getParent();
                    return row != null && row.getChildren().get(0).hasClass("field-b");
                })
                .check("field-a is the same element it was", ctx -> element(ctx, ".field-a") == fieldABefore)
                // The one that would fail if `move` were implemented as remove plus re-add.
                .checkFocused(".field-a")

                // AnimatedVisibility. Frames, not ticks: the tween runs off the frame clock.
                .checkNotExists(".fade-content")
                // Pressed rather than clicked: a full click's settles alone outlast the fade.
                .step("show the panel", ctx -> press(ctx, ".fade-toggle"))
                .frames(3)
                .checkExists(".fade-content")
                .waitUntil("the fade-in reaches full opacity", ctx -> opacity(ctx) >= 1f)

                .step("hide the panel", ctx -> press(ctx, ".fade-toggle"))
                .frames(2)
                .check("still present while fading out", ctx ->
                        ctx.query().select(".fade-content").optional().isPresent())
                .check("and already fading", ctx -> opacity(ctx) < 1f)
                .waitUntil("the fade-out finishes and unmounts it", ctx ->
                        ctx.query().select(".fade-content").optional().isEmpty())

                // Entering and leaving with a slide and a size change, moving the line below. The
                // part-way checks skip the usual settle: three settles outlast the 400ms entrance.
                .checkNotExists(".expand-content")
                .step("remember where the line below sits", ctx -> belowY = y(ctx, ".expand-below"))
                .settleMs(0).step("show the expanding panel", ctx -> press(ctx, ".expand-toggle"))
                .settleMs(0).frames(3)
                .settleMs(0).checkExists(".expand-content")
                .settleMs(0).check("the panel is part-way open", ctx -> height(ctx, ".expand-panel") < 29.5f)
                .settleMs(0).check("and drawn shifted in from the left", ctx -> panelOffsetX(ctx) < 0f)
                .screenshotElement("widgets-expanding", ".__root__")
                .waitUntil("the panel finishes opening", ctx ->
                        height(ctx, ".expand-panel") >= 29.5f && Math.abs(panelOffsetX(ctx)) < 0.01f)
                .check("the line below made room for it", ctx -> y(ctx, ".expand-below") >= belowY + 29.5f)

                .settleMs(0).step("hide the expanding panel", ctx -> press(ctx, ".expand-toggle"))
                .settleMs(0).frames(4)
                .settleMs(0).check("still mounted while it closes", ctx ->
                        ctx.query().select(".expand-content").optional().isPresent())
                .check("and leaving towards the right", ctx -> panelOffsetX(ctx) > 0f)
                .waitUntil("the panel closes and unmounts", ctx ->
                        ctx.query().select(".expand-content").optional().isEmpty())
                .check("the line below is back where it was", ctx ->
                        Math.abs(y(ctx, ".expand-below") - belowY) < 0.5f)

                // Tabs: only the selected body is displayed, but every tab stays composed.
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

                // Reordering: the headers follow the declared order and nothing is rebuilt.
                .check("headers start in declared order", ctx -> tabHeaders(ctx).equals(List.of("One", "Two")))
                .step("remember tab two's body", ctx -> tabTwoBefore = element(ctx, ".tab-body-two"))
                .click(".tab-reverse")
                .frames(3)
                .check("the headers swapped places", ctx -> tabHeaders(ctx).equals(List.of("Two", "One")))
                .check("tab two is still the one showing", ctx ->
                        paneShown(ctx, ".tab-body-two") && !paneShown(ctx, ".tab-body-one"))
                .check("tab two's body was moved, not rebuilt", ctx -> element(ctx, ".tab-body-two") == tabTwoBefore)
                // addTab appends, so a tab entering a reversed set is the case that would land last.
                .click(".tab-toggle")
                .frames(3)
                .check("a tab added to a reversed set lands first", ctx ->
                        tabHeaders(ctx).equals(List.of("Three", "Two", "One")))
                .click(".tab-toggle")
                .click(".tab-reverse")
                .frames(3)
                .check("and back to declared order", ctx -> tabHeaders(ctx).equals(List.of("One", "Two")))

                // onSelect is for the player's choices; nothing above reported one.
                .checkText(".tab-state", "selected=two reports=0")
                .step("pick tab one by its header", ctx -> pressHeader(ctx, "One"))
                .frames(3)
                .checkText(".tab-state", "selected=one reports=1")

                // Removing the selected tab hands the selection to its declared neighbour, and says so.
                .step("add tab three", ctx -> press(ctx, ".tab-toggle"))
                .frames(3)
                .step("pick it", ctx -> pressHeader(ctx, "Three"))
                .frames(3)
                .checkText(".tab-state", "selected=three reports=2")
                .step("remove it again", ctx -> press(ctx, ".tab-toggle"))
                .frames(3)
                .checkText(".tab-state", "selected=two reports=3")
                .check("and the view shows tab two", ctx -> paneShown(ctx, ".tab-body-two"))

                // The whole view leaving and coming back neither reports nor loses the selection.
                .step("hide the tab view", ctx -> press(ctx, ".tab-hide"))
                .frames(3)
                .checkNotExists(".demo-tabs")
                .checkText(".tab-state", "selected=two reports=3")
                .step("show it again", ctx -> press(ctx, ".tab-hide"))
                .frames(3)
                .check("tab two is still the one showing", ctx -> paneShown(ctx, ".tab-body-two"))
                .checkText(".tab-state", "selected=two reports=3")
                .screenshotElement("widgets-tabs", ".__root__")

                // Removing the selected tab while picking another in the same change: the caller's
                // pick wins, and the neighbour fallback neither runs nor reports.
                .step("add tab three again", ctx -> press(ctx, ".tab-toggle"))
                .frames(3)
                .step("pick it once more", ctx -> pressHeader(ctx, "Three"))
                .frames(3)
                .checkText(".tab-state", "selected=three reports=4")
                .step("close it and pick tab one", ctx -> press(ctx, ".tab-close-pick-one"))
                .frames(3)
                .checkText(".tab-state", "selected=one reports=4")
                .check("and the view shows tab one", ctx -> paneShown(ctx, ".tab-body-one"))

                // Two tabs removed at once, one of them selected: one report, naming a tab that is
                // still there.
                .step("add tab three back", ctx -> press(ctx, ".tab-toggle"))
                .frames(3)
                .step("and pick it", ctx -> pressHeader(ctx, "Three"))
                .frames(3)
                .checkText(".tab-state", "selected=three reports=5")
                .step("keep only tab one", ctx -> press(ctx, ".tab-minimal"))
                .frames(3)
                .checkText(".tab-state", "selected=one reports=6")
                .check("only tab one is left, and showing", ctx ->
                        ctx.query().type(Tab.class).count() == 1 && paneShown(ctx, ".tab-body-one"))
                .step("bring the tabs back", ctx -> press(ctx, ".tab-minimal"))
                .frames(3)
                .step("and drop tab three", ctx -> press(ctx, ".tab-toggle"))
                .frames(3)
                .checkText(".tab-state", "selected=one reports=6")

                // A modal driven by state; its content must be composed before `show()` or the body is empty.
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

                // A widget with no wrapper of its own, used through the generic escape hatch.
                .check("the unwrapped widget mounted", ctx ->
                        ctx.query().type(TreeList.class).count() == 1)
                .check("it shows the two branches it was given", ctx -> treeRows(ctx) == 2)
                .click(".tree-grow")
                .frames(3)
                .check("changing state re-drives it", ctx -> treeRows(ctx) == 4)

                // Hover as composition state: pointer, element event, composition state, recomposed text.
                .check("the card starts idle", ctx -> !hasClass(ctx, ".hover-card", "hovered"))
                .hover(".hover-card")
                .frames(3)
                .check("hovering flips the class", ctx -> hasClass(ctx, ".hover-card", "hovered"))
                .check("and the state reached the body text", ctx ->
                        text(ctx, ".hover-label").contains("hovered"))
                .hover(".remount-toggle")
                .frames(3)
                .check("leaving clears it again", ctx -> !hasClass(ctx, ".hover-card", "hovered"))

                // A nested ComposeElement removed and put back: removal disposes its composition, so
                // re-adding it has to rebuild it.
                .checkExists(".remount-body")
                .click(".remount-toggle")
                .frames(3)
                .checkNotExists(".remount-body")
                .click(".remount-toggle")
                .frames(3)
                .check("the composition was rebuilt on re-attach", ctx ->
                        ctx.query().select(".remount-body").optional().isPresent())

                // A layout block reading state nothing else reads: only the node observer can make it follow.
                .check("the observed box starts narrow", ctx -> width(ctx, ".observed-box") < 60f)
                .check("the observed text starts at its content height", ctx -> height(ctx, ".observed-text") < 20f)
                .click(".observed-toggle")
                .frames(3)
                .check("a block that reads state follows it, with no recomposition", ctx ->
                        width(ctx, ".observed-box") > 100f)
                .check("and a text given a height that way takes it", ctx ->
                        Math.abs(height(ctx, ".observed-text") - 30f) < 0.5f)
                .click(".observed-toggle")
                .frames(3)
                .check("and back again", ctx -> width(ctx, ".observed-box") < 60f)

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

    /** Presses the header captioned {@code caption}, the way a player picks a tab. */
    private static void pressHeader(TestContext ctx, String caption) {
        var header = ctx.query().type(Tab.class).list().stream()
                .map(ref -> ref.as(UIElement.class))
                .filter(tab -> caption.equals(Texts.of(tab)))
                .findFirst().orElseThrow();
        var event = UIEvent.create(UIEvents.MOUSE_DOWN);
        event.target = header;
        event.button = 0;
        UIEventDispatcher.dispatchEvent(event);
    }

    private static String text(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().text();
    }

    private static float y(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().bounds().y();
    }

    private static float height(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().bounds().height();
    }

    /** How far the expanding panel is drawn from where it is laid out, in pixels. */
    private static float panelOffsetX(TestContext ctx) {
        var panel = element(ctx, ".expand-panel");
        return panel.getStyle().transform2D().translate().resolveX(panel.getSizeWidth());
    }

    private static boolean hasClass(TestContext ctx, String selector, String styleClass) {
        return element(ctx, selector).hasClass(styleClass);
    }

    private static float width(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().bounds().width();
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
     * The tab captions in on-screen order. Walked from the tree rather than queried: a selector query
     * returns elements in the order they were first attached, which a reorder does not change.
     */
    private static List<String> tabHeaders(TestContext ctx) {
        var header = ctx.query().type(Tab.class).list().getFirst().as(UIElement.class);
        var row = header.getParent();
        return row == null ? List.of() : row.getChildren().stream().filter(Tab.class::isInstance).map(Texts::of).toList();
    }

    /**
     * Whether the pane holding {@code bodySelector} is on show. Asks the pane: {@code isDisplayed()}
     * ignores ancestors, and a tab view hides the pane, not the body.
     */
    private static boolean paneShown(TestContext ctx, String bodySelector) {
        var pane = element(ctx, bodySelector).getParent();
        return pane != null && pane.isDisplayed();
    }
}
