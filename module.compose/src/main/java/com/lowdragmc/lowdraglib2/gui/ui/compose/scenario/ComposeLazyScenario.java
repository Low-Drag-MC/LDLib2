package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestComposeLazy;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.target.Texts;

import java.util.List;

/**
 * Proves the lazy list is actually lazy, and still behaves like a list.
 *
 * <p>"It renders" is not the claim worth testing — an eager list renders too, just slowly. The claim
 * is that ten thousand items produce a couple of dozen elements, that scrolling moves which items
 * those elements show, and that the scrollbar still spans the whole list rather than the mounted
 * window.
 */
@LDLRegisterClient(name = "compose_lazy", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeLazyScenario implements UIScenario {

    /** Rows the list keeps mounted beyond a screenful; generous, because the point is the order of magnitude. */
    private static final int SANE_MOUNTED_LIMIT = 40;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("compose").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("compose lazy demo", ctx -> new TestComposeLazy().createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement(".lazy-list")
                .frames(3)

                .check("ten thousand items do not become ten thousand elements", ctx -> {
                    var mounted = ctx.query().select(".lazy-row").count();
                    return mounted > 0 && mounted < SANE_MOUNTED_LIMIT;
                })
                .check("the window starts at the top of the list", ctx ->
                        rowTexts(ctx).contains("item 0"))
                .check("the scrollbar spans the whole list, not just the window", ctx ->
                        contentHeight(ctx) > 10_000 * 14f - 1f)

                // Scrolling has to change *which* items are shown, not just move elements around.
                .step("scroll to the middle", ctx -> scrollTo(ctx, 0.5f))
                .frames(3)
                .check("the mounted window moved", ctx -> !rowTexts(ctx).contains("item 0"))
                .check("and still holds only a screenful", ctx ->
                        ctx.query().select(".lazy-row").count() < SANE_MOUNTED_LIMIT)
                .check("showing items from around the middle", ctx ->
                        rowTexts(ctx).stream().anyMatch(t -> t.startsWith("item 49")
                                || t.startsWith("item 50")))
                .screenshotElement("lazy-middle", ".__root__")

                .step("scroll to the end", ctx -> scrollTo(ctx, 1f))
                .frames(3)
                .check("the last item is reachable", ctx -> rowTexts(ctx).contains("item 9999"))

                // A grid of 2000 cells is virtualised by row, so it mounts a handful of rows worth.
                .check("the grid is virtualised too", ctx -> {
                    var cells = ctx.query().select(".grid-cell").count();
                    return cells > 0 && cells < 8 * 12;
                })
                .check("the grid shows its first row", ctx ->
                        ctx.query().select(".grid-cell").list().stream()
                                .anyMatch(ref -> ref.text().equals("c0")))

                // The horizontal variant. Checking the cells sit side by side matters: the row
                // direction has to go on the content container, not on the ScrollerView, or the
                // items stay stacked while the scrollbars go sideways instead.
                .check("the strip is virtualised", ctx -> {
                    var cells = ctx.query().select(".strip-cell").count();
                    return cells > 0 && cells < 20;
                })
                .check("its cells are laid out horizontally", ctx -> {
                    var cells = ctx.query().select(".strip-cell").list();
                    if (cells.size() < 2) return false;
                    var a = cells.get(0).bounds();
                    var b = cells.get(1).bounds();
                    return b.x() > a.x() && Math.abs(b.y() - a.y()) < 0.5f;
                })

                // Selection lives above the list, so it survives rows being recycled.
                .check("the select button starts enabled", ctx ->
                        ctx.query().select(".lazy-select").one().as(UIElement.class).isActive())
                .click(".lazy-select")
                .frames(3)
                .checkTextContains(".lazy-status", "selected 5000")
                .check("and disables itself once used", ctx ->
                        !ctx.query().select(".lazy-select").one().as(UIElement.class).isActive())
                .checkClass(".lazy-select", "__disabled__")

                // Shrinking the list below a screenful must not leave stale rows behind.
                .click(".lazy-resize")
                .frames(5)
                .checkTextContains(".lazy-status", "20 rows")
                .check("a short list mounts every row and no more", ctx ->
                        ctx.query().select(".lazy-row").count() <= 20)

                .closeScreen();
    }

    private static ScrollerView list(TestContext ctx) {
        return ctx.query().select(".lazy-list").one().as(ScrollerView.class);
    }

    private static void scrollTo(TestContext ctx, float fraction) {
        list(ctx).verticalScroller.setValue(fraction);
    }

    /** Total height of the scrolling content, spacers included. */
    private static float contentHeight(TestContext ctx) {
        float height = 0f;
        for (var child : list(ctx).viewContainer.getChildren()) {
            height += child.getSizeHeight();
        }
        return height;
    }

    private static List<String> rowTexts(TestContext ctx) {
        return ctx.query().select(".lazy-row").list().stream().map(ref -> Texts.of(ref.as(
                com.lowdragmc.lowdraglib2.gui.ui.UIElement.class))).toList();
    }
}
