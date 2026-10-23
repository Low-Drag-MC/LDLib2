package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestComposeLifecycle;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;

import java.util.List;

/** How a composition gets along with the element tree around it. */
@LDLRegisterClient(name = "compose_lifecycle", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeLifecycleScenario implements UIScenario {

    private UIElement mountedA;
    private UIElement itemA;
    private UIElement itemC;
    private UIElement movable;

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
                .checkExists(".lc-inner-manual")
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

                // Moves, a removal and an insertion in one change: the applier's batched moves have to
                // land before the removal and the insertion touch indices.
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

                // Movable content with children, carried to another parent and edited there.
                .step("remember the movable box", ctx -> movable = element(ctx, ".lc-movable"))
                .step("move it", ctx -> press(ctx, ".lc-move"))
                .frames(3)
                .check("it moved, the same element", ctx -> element(ctx, ".lc-movable") == movable
                        && movable.getParent() == element(ctx, ".lc-right"))
                .step("add a child inside it", ctx -> press(ctx, ".lc-extra"))
                .frames(3)
                .check("after its first child", ctx -> movedChildren().equals(List.of("lc-m1", "lc-m2")))
                .step("and take it out again", ctx -> press(ctx, ".lc-extra"))
                .frames(3)
                .check("only the first child is left", ctx -> movedChildren().equals(List.of("lc-m1")))
                .step("move it back", ctx -> press(ctx, ".lc-move"))
                .frames(3)
                .check("back on the left", ctx -> movable.getParent() == element(ctx, ".lc-left"))

                // A remembered element emitted again starts with only the new children.
                .step("close the card", ctx -> press(ctx, ".lc-card-toggle"))
                .frames(3)
                .step("and open it", ctx -> press(ctx, ".lc-card-toggle"))
                .frames(3)
                .check("its old child is gone", ctx -> element(ctx, ".lc-card").getChildren().size() == 1)

                // An element taken by the parent that is updated first, before the old one lets go.
                .step("move the shared element up", ctx -> press(ctx, ".lc-steal"))
                .frames(3)
                .check("it is in the top box", ctx -> element(ctx, ".lc-shared").getParent() == element(ctx, ".lc-top"))
                .checkExists(".lc-tail")
                .step("and back down", ctx -> press(ctx, ".lc-steal"))
                .frames(3)
                .check("before the tail again", ctx -> {
                    var bottom = element(ctx, ".lc-bottom").getChildren();
                    return bottom.size() == 2 && bottom.get(0) == element(ctx, ".lc-shared");
                })

                .checkText(".lc-effect", "first effect attached=true")

                // Transitions dropped while shown: what they wrote goes, rather than becoming defaults.
                .check("the transitions write opacity and height", ctx ->
                        inline(ctx, ".lc-av", PropertyRegistry.OPACITY) && inline(ctx, ".lc-av", LayoutProperties.HEIGHT))
                .step("drop them", ctx -> press(ctx, ".lc-plain"))
                .frames(3)
                .check("and what they wrote is gone", ctx ->
                        !inline(ctx, ".lc-av", PropertyRegistry.OPACITY) && !inline(ctx, ".lc-av", LayoutProperties.HEIGHT))

                // Pressed survives the pointer moving onto the tracked element's own child.
                .step("press on the tracked box's padding", ctx -> {
                    var box = ctx.query().select(".lc-track").one().bounds();
                    ctx.input().moveTo(box.x() + 2, box.y() + 2);
                    ctx.input().mouseDown(box.x() + 2, box.y() + 2, Keys.MOUSE_LEFT);
                })
                .frames(3)
                .checkText(".lc-pressed", "pressed=true")
                .step("move onto its child", ctx -> {
                    var child = ctx.query().select(".lc-track-child").one().bounds();
                    ctx.input().moveTo(child.centerX(), child.centerY());
                })
                .frames(3)
                .checkText(".lc-pressed", "pressed=true")
                .step("release", ctx -> {
                    var child = ctx.query().select(".lc-track-child").one().bounds();
                    ctx.input().mouseUp(child.centerX(), child.centerY(), Keys.MOUSE_LEFT);
                })
                .frames(3)
                .checkText(".lc-pressed", "pressed=false")

                .closeScreen();
    }

    /** Which of the movable box's two children it holds, in order. */
    private List<String> movedChildren() {
        return movable.getChildren().stream()
                .map(child -> child.getClasses().stream().filter(c -> c.startsWith("lc-m")).findFirst().orElse("?"))
                .toList();
    }

    private static boolean inline(TestContext ctx, String selector, Property<?> property) {
        return element(ctx, selector).getStyleBag().containsCandidate(property, slot -> slot.origin() == StyleOrigin.INLINE);
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
