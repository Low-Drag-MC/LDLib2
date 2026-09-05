package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;

/**
 * Holding shift turns the wheel sideways in a {@link ScrollerView} that scrolls both ways.
 *
 * <p>Almost no mouse has a horizontal wheel, so a both-directions view could previously only be moved
 * sideways by dragging its scroll bar. Shift is what every desktop application uses instead.
 *
 * <p>Each check names both axes, because the interesting failure is not "it did not scroll" but "it
 * scrolled the wrong one" — and a test that only looked at the axis it expected to move would pass
 * just as happily if both moved together.
 *
 * <p>Shift is held as a real key rather than declared in the event's modifier mask: the swap reads
 * held key state through {@code KeyState}, which is also the only reading that is correct for a UI in
 * a window of its own, since {@code glfwGetKey} answers per window.
 */
@LDLRegisterClient(name = "scroller_shift_axis", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ScrollerShiftAxisScenario implements UIScenario {

    private static final String VIEW = "scroller_view";
    private static final String BEFORE = "scroller_values_before";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("scroller", "input").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("scroller", ctx -> new ModularUI(UI.of(root(ctx)), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#scroll_view")
                .waitUntil("the content overflows both ways, so both axes can actually move", ctx ->
                        view(ctx).getContainerWidth() > view(ctx).viewPort.getContentWidth()
                                && view(ctx).getContainerHeight() > view(ctx).viewPort.getContentHeight())
                .check("both scrollers start at the origin",
                        ctx -> vertical(ctx) == 0f && horizontal(ctx) == 0f)

                .group("a plain wheel still scrolls vertically", g -> g
                        // Negative is "away from the user", which the scroller turns into a move
                        // towards the end of its range.
                        .scroll("#scroll_view", -1)
                        .frames(3)
                        .check("the vertical scroller moved", ctx -> vertical(ctx) > 0f)
                        .check("and the horizontal one did not", ctx -> horizontal(ctx) == 0f))

                .group("shift sends the same wheel sideways", g -> g
                        .step("remember where both scrollers are", ctx -> ctx.put(BEFORE,
                                new float[]{vertical(ctx), horizontal(ctx)}))
                        .keyDown(Keys.LEFT_SHIFT)
                        .scroll("#scroll_view", -1)
                        .keyUp(Keys.LEFT_SHIFT)
                        .frames(3)
                        .check("the horizontal scroller moved", ctx -> horizontal(ctx) > 0f)
                        .check("and the vertical one stayed exactly where it was",
                                ctx -> vertical(ctx) == ((float[]) ctx.get(BEFORE))[0]))

                .group("and the other way brings it back", g -> g
                        .keyDown(Keys.LEFT_SHIFT)
                        .scroll("#scroll_view", 1)
                        .keyUp(Keys.LEFT_SHIFT)
                        .frames(3)
                        .check("the horizontal scroller is back at the origin",
                                ctx -> horizontal(ctx) == 0f))

                .group("shift is ignored where there is nothing to swap onto", g -> g
                        .step("make it scroll vertically only",
                                ctx -> view(ctx).getScrollerViewStyle().mode(ScrollerMode.VERTICAL))
                        .frames(3)
                        .step("remember where the vertical scroller is",
                                ctx -> ctx.put(BEFORE, new float[]{vertical(ctx), horizontal(ctx)}))
                        .keyDown(Keys.LEFT_SHIFT)
                        .scroll("#scroll_view", -1)
                        .keyUp(Keys.LEFT_SHIFT)
                        .frames(3)
                        // Eating shift+wheel over a plain list would be a regression, not a feature.
                        .check("shift+wheel still scrolls it",
                                ctx -> vertical(ctx) > ((float[]) ctx.get(BEFORE))[0]))

                .teardown("close the screen", ctx -> ctx.mc().gui.setScreen(null));
    }

    // ------------------------------------------------------------------------------------ fixtures

    private static UIElement root(TestContext ctx) {
        // Bigger than the viewport in both directions, so both scrollers have somewhere to go.
        var content = new UIElement();
        content.setId("content")
                .layout(layout -> layout.width(600).height(600))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF3A5F7D)));

        var view = new ScrollerView();
        view.setId("scroll_view")
                .layout(layout -> layout.width(160).height(120))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF1B1B22)));
        view.getScrollerViewStyle().mode(ScrollerMode.BOTH);
        view.addScrollViewChild(content);
        ctx.put(VIEW, view);

        var root = new UIElement();
        root.setId("panel")
                .layout(layout -> layout.widthPercent(100).heightPercent(100).paddingAll(20))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF101014)))
                .addChild(view);
        return root;
    }

    // ------------------------------------------------------------------------------------- helpers

    private static ScrollerView view(TestContext ctx) {
        return ctx.get(VIEW);
    }

    private static float vertical(TestContext ctx) {
        return view(ctx).verticalScroller.getValue();
    }

    private static float horizontal(TestContext ctx) {
        return view(ctx).horizontalScroller.getValue();
    }
}
