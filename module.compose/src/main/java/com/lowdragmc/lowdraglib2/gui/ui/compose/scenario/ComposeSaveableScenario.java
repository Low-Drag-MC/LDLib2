package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.compose.ComposeSavedState;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestComposeSaveable;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
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
 * {@code rememberSaveable} against a real screen being closed and opened again.
 *
 * <p>The pure-JVM tests cover the registry; what only a real screen can show is that closing it
 * actually reaches the save — the composition is torn down from {@code onRemoved}, and the state has
 * to be collected before that, while everything is still registered — and that the element built for
 * the reopened screen picks it up.
 *
 * <p>The plain {@code remember} counter is the control: it must come back at zero, or the screen was
 * never really rebuilt and the saveable check proves nothing.
 */
@LDLRegisterClient(name = "compose_saveable", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeSaveableScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("compose").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        // Saved state lives for the client session, so a previous run would otherwise leak in.
        s.step("start with nothing saved", ctx -> ComposeSavedState.clear(TestComposeSaveable.STATE_KEY));
        open(s);

        s.step("count twice on each side", ctx -> {
                    press(ctx, ".kept");
                    press(ctx, ".kept");
                    press(ctx, ".lost");
                    press(ctx, ".lost");
                })
                .frames(3)
                .checkText(".kept", "saveable 2")
                .checkText(".lost", "remember 2")

                // A row's own state across the row leaving the composition and coming back.
                .step("star the first row", ctx -> press(ctx, ".star-row-0"))
                .frames(3)
                .checkText(".star-row-0", "* row 0")
                .step("scroll the list to the end", ctx -> list(ctx).verticalScroller.setValue(1f))
                .frames(5)
                .check("the first row has left the composition", ctx ->
                        ctx.query().select(".star-row-0").optional().isEmpty())
                .step("scroll back to the top", ctx -> list(ctx).verticalScroller.setValue(0f))
                .frames(5)
                .checkText(".star-row-0", "* row 0")
                .checkText(".star-row-1", "row 1")

                // A tab's state across the tab being removed and declared again.
                .step("count three in tab B", ctx -> {
                    press(ctx, ".tab-counter-b");
                    press(ctx, ".tab-counter-b");
                    press(ctx, ".tab-counter-b");
                })
                .frames(3)
                .checkText(".tab-counter-b", "b 3")
                .step("remove tab B", ctx -> press(ctx, ".toggle-b"))
                .frames(3)
                .checkNotExists(".tab-counter-b")
                .step("add tab B back", ctx -> press(ctx, ".toggle-b"))
                .frames(3)
                .checkText(".tab-counter-b", "b 3")
                .checkText(".tab-counter-a", "a 0")

                .closeScreen()
                .frames(3);

        // Everything above, through a reopen.
        open(s);
        s.checkText(".kept", "saveable 2")
                .checkText(".lost", "remember 0")
                .checkText(".star-row-0", "* row 0")
                .checkText(".tab-counter-b", "b 3")
                .checkText(".tab-counter-a", "a 0")
                .closeScreen()
                .frames(3)
                .step("forget the saved state", ctx -> ComposeSavedState.clear(TestComposeSaveable.STATE_KEY));

        open(s);
        s.checkText(".kept", "saveable 0")
                .checkText(".star-row-0", "row 0")
                .checkText(".tab-counter-b", "b 0")
                .closeScreen();
    }

    private static void open(ScenarioBuilder s) {
        s.openModularUI("compose saveable demo", ctx -> new TestComposeSaveable().createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement(".kept")
                .frames(3);
    }

    private static ScrollerView list(TestContext ctx) {
        return ctx.query().select(".starred-list").one().as(ScrollerView.class);
    }

    /**
     * Fires a left mouse-down straight at an element, which is what a Button acts on. Dispatched
     * rather than clicked so a button inside a tab that is not showing can still be pressed.
     */
    private static void press(TestContext ctx, String selector) {
        var event = UIEvent.create(UIEvents.MOUSE_DOWN);
        event.target = ctx.query().select(selector).one().as(UIElement.class);
        event.button = 0;
        UIEventDispatcher.dispatchEvent(event);
    }
}
