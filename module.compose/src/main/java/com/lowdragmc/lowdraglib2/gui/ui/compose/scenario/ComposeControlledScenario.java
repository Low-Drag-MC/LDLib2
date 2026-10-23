package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestComposeControlled;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.target.Texts;

import java.util.List;

/** Controlled widgets where the composition and the widget could disagree. */
@LDLRegisterClient(name = "compose_controlled", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeControlledScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("compose").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("compose controlled demo", ctx -> new TestComposeControlled().createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement(".c-root")
                .frames(3)

                // A dialog visible from the first frame: show() hides rows that are empty at that
                // moment, so slots filled too late would leave it blank.
                .check("a dialog open from the start shows its content", ctx -> paneShown(ctx, ".c-dialog-body"))
                .check("and its title", ctx -> paneShown(ctx, ".c-dialog-title"))
                .check("and its buttons", ctx -> paneShown(ctx, ".c-dialog-close"))
                .check("its layout went on the window, not the backdrop", ctx ->
                        Math.abs(ctx.query().select(".c-dialog").one().bounds().width() - 160f) < 0.5f)
                .screenshotElement("controlled-dialog", ".__root__")

                // Escape dismisses it rather than the screen: it is shown only once a UI can hold focus.
                .key(Keys.ESCAPE)
                .frames(3)
                .checkScreen(ModularUIScreen.class)
                .checkNotExists(".c-dialog-body")
                .checkText(".c-dismissed", "dismissed=1")
                .step("open it after Escape", ctx -> press(ctx, ".c-dialog-open"))
                .frames(3)
                .checkExists(".c-dialog-body")

                // Closed by the caller's own state: not a dismissal. A real click, as a press without
                // moving the pointer looks to the dialog like a click outside it.
                .step("close it from its own button", ctx -> clickOnce(ctx, ".c-dialog-close"))
                .frames(3)
                .checkNotExists(".c-dialog-body")
                .checkText(".c-dismissed", "dismissed=1")

                // Closed by the player clicking outside it: a dismissal.
                .step("open it again", ctx -> press(ctx, ".c-dialog-open"))
                .frames(3)
                .checkExists(".c-dialog-body")
                .step("click outside the window", ctx -> {
                    var root = ctx.query().select(".c-root").one().bounds();
                    ctx.input().mouseDown(root.x() + 2, root.y() + 2, Keys.MOUSE_LEFT);
                    ctx.input().mouseUp(root.x() + 2, root.y() + 2, Keys.MOUSE_LEFT);
                })
                .frames(3)
                .checkNotExists(".c-dialog-body")
                .checkText(".c-dismissed", "dismissed=2")

                // A slot that empties while the dialog shows takes its row with it, and one that fills
                // brings it back - including when the dialog was opened with it empty.
                .step("open it for the title checks", ctx -> press(ctx, ".c-dialog-open"))
                .frames(3)
                .step("drop the title", ctx -> clickOnce(ctx, ".c-dialog-retitle"))
                .frames(3)
                .check("the empty title row is hidden", ctx -> !titleRow(ctx).isDisplayed())
                .step("close it", ctx -> clickOnce(ctx, ".c-dialog-close"))
                .frames(3)
                .step("open it untitled", ctx -> press(ctx, ".c-dialog-open"))
                .frames(3)
                .step("give it its title back", ctx -> clickOnce(ctx, ".c-dialog-retitle"))
                .frames(3)
                .check("the title row shows again", ctx -> paneShown(ctx, ".c-dialog-title"))
                .step("close it again", ctx -> clickOnce(ctx, ".c-dialog-close"))
                .frames(3)
                .checkText(".c-dismissed", "dismissed=2")

                // An edit the caller refuses by not storing it is undone, keystroke by keystroke.
                .typeInto(".c-short", "abcdefgh")
                .frames(3)
                .check("the field holds only what the state accepted", ctx -> "abcde".equals(value(ctx, ".c-short")))
                .checkTextContains(".c-text-state", "short=abcde")

                // A character filter refuses before the character lands.
                .typeInto(".c-digits", "4a5")
                .frames(3)
                .check("the filter kept the letter out", ctx -> "45".equals(value(ctx, ".c-digits")))
                // A validator lets the text through as an error, and only reports valid text.
                .typeInto(".c-three", "4567")
                .frames(3)
                .checkTextContains(".c-text-state", "three=456")

                // A correction applied while typing keeps the caret where it was.
                .focus(".c-shout")
                .type("ab")
                .frames(3)
                .check("the area holds the caller's correction", ctx ->
                        List.of("AB").equals(List.of((String[]) value(ctx, ".c-shout"))))
                .check("and the caret is still after what was typed", ctx ->
                        element(ctx, ".c-shout", TextArea.class).cursorPos().col() == 2)

                // A value written together with its range lands in that range, not in the default.
                .check("the slider starts at 50 of 0..100", ctx -> Float.valueOf(50f).equals(value(ctx, ".c-slider")))
                .check("and so does the progress bar", ctx ->
                        Math.abs(element(ctx, ".c-progress", ProgressBar.class).getNormalizedValue() - 0.5f) < 0.001f)
                .step("widen the range and move the value with it", ctx -> press(ctx, ".c-widen"))
                .frames(3)
                .check("the slider follows to 500 of 0..1000", ctx -> Float.valueOf(500f).equals(value(ctx, ".c-slider")))
                // The range alone: clamped on the way down, back where the state says on the way up,
                // for the bar too, which keeps the clamped value unless told it again.
                .step("shrink the range under the value", ctx -> press(ctx, ".c-shrink"))
                .frames(3)
                .check("the slider is clamped to 100", ctx -> Float.valueOf(100f).equals(value(ctx, ".c-slider")))
                .step("restore the range", ctx -> press(ctx, ".c-restore"))
                .frames(3)
                .check("the slider is back at 500", ctx -> Float.valueOf(500f).equals(value(ctx, ".c-slider")))
                .check("and so is the progress bar", ctx ->
                        Math.abs(element(ctx, ".c-progress", ProgressBar.class).getNormalizedValue() - 0.5f) < 0.001f)

                // Candidates changed in place, through a mutableStateListOf, reach the drop-down.
                .check("the selector starts with one candidate", ctx -> candidates(ctx) == 1)
                .step("add a candidate in place", ctx -> press(ctx, ".c-add-option"))
                .frames(3)
                .check("and then has two", ctx -> candidates(ctx) == 2)

                // A label that goes away and comes back.
                .step("make the button busy", ctx -> press(ctx, ".c-busy"))
                .frames(3)
                .check("its label is hidden", ctx -> !element(ctx, ".c-busy", Button.class).text.isDisplayed())
                .step("and ready again", ctx -> press(ctx, ".c-busy"))
                .frames(3)
                .check("its label is back", ctx -> {
                    var button = element(ctx, ".c-busy", Button.class);
                    return button.text.isDisplayed() && "ready".equals(Texts.of(button));
                })
                .check("a toggle without text hides its label", ctx ->
                        !element(ctx, ".c-bare-toggle", Toggle.class).toggleLabel.isDisplayed())
                .click(".c-switch")
                .frames(3)
                .checkText(".c-on", "on=true")

                // A toggle group's side effects are not edits.
                .checkText(".c-pick-state", "pick=a reports=0")
                .step("pick c from state", ctx -> press(ctx, ".c-pick-c"))
                .frames(3)
                .checkText(".c-pick-state", "pick=c reports=0")
                .check("the group shows c checked and a not", ctx ->
                        isOn(ctx, ".c-toggle-c") && !isOn(ctx, ".c-toggle-a"))
                // A click reports to the toggle clicked, not to the one the group unchecks for it.
                .click(".c-toggle-b")
                .frames(3)
                .checkText(".c-pick-state", "pick=b reports=1")
                .check("and b is the one on", ctx -> isOn(ctx, ".c-toggle-b") && !isOn(ctx, ".c-toggle-c"))
                // A refused check puts the previous toggle back.
                .click(".c-toggle-d")
                .frames(3)
                .checkText(".c-pick-state", "pick=b reports=2")
                .check("the refused toggle is off and b is on again", ctx ->
                        isOn(ctx, ".c-toggle-b") && !isOn(ctx, ".c-toggle-d"))
                // Removing the checked toggle makes the group check another by itself. That is put
                // back to what the state says - none, as the state still names the removed one.
                .step("drop the checked toggle", ctx -> press(ctx, ".c-drop-picked"))
                .frames(3)
                .checkNotExists(".c-toggle-b")
                .check("no toggle shows checked", ctx ->
                        !isOn(ctx, ".c-toggle-a") && !isOn(ctx, ".c-toggle-c") && !isOn(ctx, ".c-toggle-d"))
                .checkText(".c-pick-state", "pick=b reports=2")
                .click(".c-toggle-a")
                .frames(3)
                .checkText(".c-pick-state", "pick=a reports=3")
                .check("and the toggle the group had checked can be picked", ctx -> isOn(ctx, ".c-toggle-a"))

                // Spacing given to the scrolled content lands between the rows.
                .check("the rows are spaced by contentLayout", ctx -> {
                    var first = ctx.query().select(".c-row-1").one().bounds();
                    var second = ctx.query().select(".c-row-2").one().bounds();
                    return Math.abs(second.y() - first.bottom() - 5f) < 0.5f;
                })

                .closeScreen();
    }

    private static UIElement element(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().as(UIElement.class);
    }

    private static <T extends UIElement> T element(TestContext ctx, String selector, Class<T> type) {
        return ctx.query().select(selector).one().as(type);
    }

    private static Object value(TestContext ctx, String selector) {
        return ((BindableUIElement<?>) element(ctx, selector)).getValue();
    }

    private static boolean isOn(TestContext ctx, String selector) {
        return Boolean.TRUE.equals(value(ctx, selector));
    }

    /** The dialog window's title row; the window is the element the dialog's classes go on. */
    private static UIElement titleRow(TestContext ctx) {
        return element(ctx, ".c-dialog").getChildren().get(0);
    }

    private static int candidates(TestContext ctx) {
        return element(ctx, ".c-selector", Selector.class).getCandidates().size();
    }

    /** Whether the container an element sits in is displayed - the dialog hides empty rows. */
    private static boolean paneShown(TestContext ctx, String selector) {
        var parent = element(ctx, selector).getParent();
        return parent != null && parent.isDisplayed();
    }

    /**
     * Presses and releases over an element, resolving its position once: the dialog's own button
     * removes itself on the press, so the release cannot look it up again.
     */
    private static void clickOnce(TestContext ctx, String selector) {
        var bounds = ctx.query().select(selector).one().bounds();
        ctx.input().moveTo(bounds.centerX(), bounds.centerY());
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
    }

    /** Fires a left mouse-down straight at an element, which is what a Button acts on. */
    private static void press(TestContext ctx, String selector) {
        var event = UIEvent.create(UIEvents.MOUSE_DOWN);
        event.target = element(ctx, selector);
        event.button = 0;
        UIEventDispatcher.dispatchEvent(event);
    }
}
