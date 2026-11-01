package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

/**
 * A {@link ScrollerView} that sizes itself to its content along one axis.
 *
 * <p>Two ways to get it wrong, one guarded against each way:
 * <ul>
 *   <li>Under a hidden ancestor it is 0x0, and deriving its chrome from that bakes a height with no
 *       padding into it for good — so it must not adapt then.</li>
 *   <li>Laid out empty and filled afterwards it is 0 along the adapted axis only, and refusing to
 *       adapt then left it 0 for good: the tab strip of a floated editor pane, built before its first
 *       tab, never appeared.</li>
 * </ul>
 */
@LDLRegisterClient(name = "scroller_adaptive_size", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ScrollerAdaptiveSizeScenario implements UIScenario {

    private static final String EMPTY = "empty_scroller";
    private static final String HIDDEN_PARENT = "hidden_parent";
    private static final String HIDDEN = "hidden_scroller";
    private static final float ITEM_HEIGHT = 18;
    /** Padding gives a scroller height even when empty, which would hide the first failure. */
    private static final float PADDING = 2;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("scroller", "layout").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("scrollers", ctx -> {
                    var empty = strip(0);
                    var hidden = strip(PADDING);
                    hidden.addScrollViewChild(item());
                    var hiddenParent = new UIElement().layout(layout -> layout.width(200)).addChild(hidden);
                    hiddenParent.setDisplay(false);
                    ctx.put(EMPTY, empty);
                    ctx.put(HIDDEN, hidden);
                    ctx.put(HIDDEN_PARENT, hiddenParent);
                    var root = new UIElement().layout(layout -> layout.width(220).height(120).gapAll(4))
                            .addChildren(empty, hiddenParent);
                    return new ModularUI(UI.of(root), ctx.player());
                })
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .frames(5)

                .group("one laid out empty grows when content arrives", g -> g
                        .check("it is laid out across, and empty", ctx -> scroller(ctx, EMPTY).getSizeWidth() > 0)
                        .step("add an item", ctx -> scroller(ctx, EMPTY).addScrollViewChild(item()))
                        .frames(5)
                        .check("it fits the item", ctx -> fits(scroller(ctx, EMPTY), 0)))

                .group("one under a hidden ancestor keeps its padding once shown", g -> g
                        .check("hidden, it is 0x0", ctx -> scroller(ctx, HIDDEN).getSizeWidth() == 0
                                && scroller(ctx, HIDDEN).getSizeHeight() == 0)
                        .step("show its parent", ctx -> ctx.<UIElement>get(HIDDEN_PARENT).setDisplay(true))
                        .frames(5)
                        .check("it fits the item and its padding", ctx -> fits(scroller(ctx, HIDDEN), PADDING)));
    }

    /** Shaped like an editor pane's tab strip. */
    private static ScrollerView strip(float padding) {
        var scroller = new ScrollerView();
        scroller.viewPort(viewPort -> viewPort.layout(layout -> layout.paddingAll(0)))
                .scrollerStyle(style -> style.mode(ScrollerMode.HORIZONTAL)
                        .horizontalScrollDisplay(ScrollDisplay.NEVER).adaptiveHeight(true))
                .layout(layout -> layout.widthPercent(100).paddingAll(padding));
        return scroller;
    }

    private static UIElement item() {
        return new UIElement().layout(layout -> layout.width(40).height(ITEM_HEIGHT));
    }

    private static ScrollerView scroller(TestContext ctx, String key) {
        return ctx.get(key);
    }

    private static boolean fits(ScrollerView scroller, float padding) {
        return Math.abs(scroller.getSizeHeight() - (ITEM_HEIGHT + 2 * padding)) < 0.5f;
    }
}
