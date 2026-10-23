package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestComposeLifecycle;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

/**
 * How a composition gets along with the element tree around it.
 *
 * <p>The applier only ever touches what it inserted, an adopted element can be swapped, a nested
 * composition comes back after leaving the tree, and a composition that fails - its content, or one
 * node's style block - fails on its own while the rest of the screen keeps working.
 */
@LDLRegisterClient(name = "compose_lifecycle", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeLifecycleScenario implements UIScenario {

    private UIElement mountedA;
    private UIElement itemA;
    private UIElement itemC;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("compose").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("compose lifecycle demo", ctx -> new TestComposeLifecycle().createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement(".lc-root")
                .frames(3)

                // A child added by hand sits after the composition's own and survives its updates.
                .checkExists(".lc-manual")
                .check("the hand-added child comes after the composed ones", ctx -> lastChildIsManual(ctx))
                .step("recompose a few times", ctx -> {
                    press(ctx, ".lc-recompose");
                    press(ctx, ".lc-recompose");
                    press(ctx, ".lc-recompose");
                })
                .frames(3)
                .checkTextContains(".lc-state", "ticks=3")
                .checkExists(".lc-manual")

                // Mount swaps the element it adopts when it is handed a different one.
                .checkExists(".lc-mount-a")
                .step("remember the adopted element", ctx -> mountedA = element(ctx, ".lc-mount-a"))
                .step("swap", ctx -> press(ctx, ".lc-swap"))
                .frames(3)
                .checkExists(".lc-mount-b")
                .check("the first element left the tree", ctx -> mountedA.getParent() == null)
                .step("and back", ctx -> press(ctx, ".lc-swap"))
                .frames(3)
                .check("the same first element is back", ctx -> element(ctx, ".lc-mount-a") == mountedA)
                .checkExists(".lc-manual")

                // A nested composition that leaves the tree is torn down, and rebuilt when it returns.
                .step("count in the nested composition", ctx -> press(ctx, ".lc-inner-button"))
                .frames(3)
                .checkText(".lc-inner-button", "inner 1")
                .step("take it out of the tree", ctx -> press(ctx, ".lc-toggle-inner"))
                .frames(3)
                .checkNotExists(".lc-inner-button")
                .step("and put it back", ctx -> press(ctx, ".lc-toggle-inner"))
                .frames(3)
                .check("it was rebuilt from its content", ctx ->
                        ctx.query().select(".lc-inner-button").optional().isPresent())
                .checkText(".lc-inner-button", "inner 0")
                .step("and it still recomposes", ctx -> press(ctx, ".lc-inner-button"))
                .frames(3)
                .checkText(".lc-inner-button", "inner 1")

                // Content that throws leaves its own element empty, and only that.
                .checkExists(".lc-broken")
                .check("the failing composition stayed empty", ctx -> element(ctx, ".lc-broken").getChildren().isEmpty())

                // A style block that throws, both when re-run on its own and during a recomposition.
                .step("make the style block throw", ctx -> press(ctx, ".lc-explode"))
                .frames(3)
                .step("and recompose around it", ctx -> press(ctx, ".lc-recompose"))
                .frames(3)
                .checkTextContains(".lc-state", "ticks=4")
                .step("stop it throwing", ctx -> press(ctx, ".lc-explode"))
                .frames(3)
                .step("recompose again", ctx -> press(ctx, ".lc-recompose"))
                .frames(3)
                .checkTextContains(".lc-state", "ticks=5")

                // One listener, however many recompositions re-declared it.
                .step("click the clicker once", ctx -> press(ctx, ".lc-clicker"))
                .frames(3)
                .checkTextContains(".lc-state", "clicks=1")

                // The state bridges follow a TrackData both ways.
                .step("write through the bridge", ctx -> press(ctx, ".lc-bridge-add"))
                .frames(3)
                .checkText(".lc-bridge-state", "bridged=1 collected=1")
                .step("write the data from outside", ctx -> press(ctx, ".lc-bridge-set"))
                .frames(3)
                .checkText(".lc-bridge-state", "bridged=100 collected=100")

                // Moves, a removal and an insertion in one change. The applier holds moves back and
                // applies them as one permutation, and has to land them before it touches indices
                // for the removal and the insertion.
                .step("remember two rows that survive", ctx -> {
                    itemA = element(ctx, ".lc-item-a");
                    itemC = element(ctx, ".lc-item-c");
                })
                .step("reshuffle", ctx -> press(ctx, ".lc-reshuffle"))
                .frames(3)
                .check("the rows are in the new order", ctx -> {
                    var row = element(ctx, ".lc-items");
                    var texts = row.getChildren().stream().map(com.lowdragmc.lowdraglib2.uitest.target.Texts::of).toList();
                    return texts.equals(java.util.List.of("d", "c", "a", "e"));
                })
                .check("the rows that stayed were moved, not rebuilt", ctx ->
                        element(ctx, ".lc-item-a") == itemA && element(ctx, ".lc-item-c") == itemC)
                .checkNotExists(".lc-item-b")

                .closeScreen();
    }

    private static UIElement element(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().as(UIElement.class);
    }

    private static boolean lastChildIsManual(TestContext ctx) {
        var manual = element(ctx, ".lc-manual");
        var host = manual.getParent();
        if (host == null) return false;
        var children = host.getChildren();
        return children.get(children.size() - 1) == manual && children.size() > 1;
    }

    /** Fires a left mouse-down straight at an element, which is what a Button acts on. */
    private static void press(TestContext ctx, String selector) {
        var event = UIEvent.create(UIEvents.MOUSE_DOWN);
        event.target = element(ctx, selector);
        event.button = 0;
        UIEventDispatcher.dispatchEvent(event);
    }
}
