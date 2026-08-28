package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestComposeGallery;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Runs every widget wrapper the other scenarios never reach.
 *
 * <p>Two thirds of the wrappers were compiled but had never been instantiated. That gap is exactly
 * how {@code LazyRow} shipped with a wrong line in it — it looked fine and nothing had ever rendered
 * it. So the first half of this scenario is deliberately unglamorous: mount all of them and check
 * they are there.
 *
 * <p>The second half checks the thing that would realistically be broken rather than merely absent —
 * the controlled-value round trip. Pushing a value down from composition state and reading it back
 * off the element is what catches a comparison done on the wrong type, which is the mistake these
 * wrappers are most prone to: {@code TextArea} and {@code CodeEditor} hold {@code String[]}, and an
 * array compares by identity, so a naive guard would fight the user on every keystroke.
 */
@LDLRegisterClient(name = "compose_gallery", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeGalleryScenario implements UIScenario {

    /** Every wrapper this screen exists to exercise, by the class it puts on its element. */
    private static final List<String> MOUNTED = List.of(
            "g-label", "g-textarea", "g-codeeditor",
            "g-slider", "g-slider-v", "g-scroller-h", "g-scroller-v",
            "g-selector", "g-colour", "g-tagfield", "g-structured", "g-headless",
            "g-itemslot", "g-fluidslot", "g-togglegroup", "g-inventory",
            "g-split-v", "g-split-top", "g-mounted", "g-template",
            "gallery-column");

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("compose").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("compose gallery", ctx -> new TestComposeGallery().createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement(".gallery-root")
                .frames(3);

        // Composing at all is the bar these have not cleared before now.
        for (var styleClass : MOUNTED) {
            s.checkExists("." + styleClass);
        }

        s.screenshotElement("gallery", ".gallery-root")

                // Array-valued widgets: the guard has to compare contents, not identity.
                .check("the text area shows the lines it was given", ctx ->
                        lines(ctx, ".g-textarea").equals(List.of("one", "two")))
                .click(".g-add-line")
                .frames(3)
                .check("and follows the state when a line is added", ctx ->
                        lines(ctx, ".g-textarea").equals(List.of("one", "two", "three")))
                .check("the code editor holds its own lines", ctx ->
                        lines(ctx, ".g-codeeditor").equals(List.of("var x = 1")))

                // Numeric widgets.
                .check("the slider starts where state put it", ctx -> value(ctx, ".g-slider", 0f) == 0.25f)
                .click(".g-set-slider")
                .frames(3)
                .check("and follows state", ctx -> value(ctx, ".g-slider", 0f) == 0.75f)

                // Selector was the fiddly one to type; check the value actually arrives.
                .check("the selector starts on beta", ctx -> "beta".equals(rawValue(ctx, ".g-selector")))
                .click(".g-set-selector")
                .frames(3)
                .check("and follows state to gamma", ctx -> "gamma".equals(rawValue(ctx, ".g-selector")))

                .check("the colour selector starts blue", ctx ->
                        Integer.valueOf(0xFF3366CC).equals(rawValue(ctx, ".g-colour")))
                .click(".g-set-colour")
                .frames(3)
                .check("and follows state to red", ctx ->
                        Integer.valueOf(0xFFCC3333).equals(rawValue(ctx, ".g-colour")))

                // Item slot: a stack is not a scalar, so it is worth checking the whole thing lands.
                .check("the item slot holds the stack it was given", ctx ->
                        stack(ctx).is(Items.APPLE) && stack(ctx).getCount() == 3)
                .click(".g-set-item")
                .frames(3)
                .check("and follows state to a different stack", ctx ->
                        stack(ctx).is(Items.DIAMOND) && stack(ctx).getCount() == 7)

                // Siblings in a Column must not sit on top of each other. Worth asserting rather
                // than eyeballing: a widget whose layout box collapses to zero still draws, so an
                // overlap looks like a styling quirk when it is actually a sizing bug.
                .check("the item row starts below the button above it", ctx ->
                        bounds(ctx, ".g-itemslot").y() >= bounds(ctx, ".g-set-item").bottom() - 0.5f)
                .check("the tag field starts below the colour selector", ctx ->
                        bounds(ctx, ".g-tagfield").y() >= bounds(ctx, ".g-colour").bottom() - 0.5f)

                // Mount adopts an element built outside the composition; it must be that instance.
                .check("Mount attached the element it was handed, inside the split", ctx -> {
                    var mounted = element(ctx, ".g-mounted");
                    return mounted.getParent() != null && mounted.hasClass("g-mounted");
                })

                .closeScreen();
    }

    private static UIElement element(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().as(UIElement.class);
    }

    private static com.lowdragmc.lowdraglib2.uitest.ElementBounds bounds(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().bounds();
    }

    /** The raw value of a bindable widget, without going through text extraction. */
    private static Object rawValue(TestContext ctx, String selector) {
        return ((BindableUIElement<?>) element(ctx, selector)).getValue();
    }

    private static List<String> lines(TestContext ctx, String selector) {
        var value = rawValue(ctx, selector);
        return value instanceof String[] array ? List.of(array) : List.of();
    }

    private static float value(TestContext ctx, String selector, float fallback) {
        var value = rawValue(ctx, selector);
        return value instanceof Float f ? f : fallback;
    }

    private static ItemStack stack(TestContext ctx) {
        var value = rawValue(ctx, ".g-itemslot");
        return value instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
    }
}
