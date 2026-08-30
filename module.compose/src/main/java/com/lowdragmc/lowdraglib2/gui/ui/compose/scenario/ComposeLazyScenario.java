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

                // Variable-height rows. Nothing tells the list how tall a row is; it measures the
                // ones it mounts and carries an estimate for the rest, so the interesting question
                // is whether the spacers it computes from that mixture leave the rows laid out
                // correctly - a wrong spacer shows up as rows overlapping or a gap between them.
                .check("the ragged list is virtualised too", ctx -> {
                    var mounted = ctx.query().select(".ragged-row").count();
                    return mounted > 0 && mounted < SANE_MOUNTED_LIMIT;
                })
                .check("its rows really are different heights", ctx ->
                        raggedHeights(ctx).stream().distinct().count() > 1)
                .check("rows sit one after another with no overlap and no gap", ctx ->
                        raggedRowsAreContiguous(ctx))
                .check("the scrollbar spans the whole ragged list", ctx ->
                        raggedContentHeight(ctx) > 400 * 10f)
                // At the top there is nothing above the first row, so the leading spacer must be
                // flat zero. This is the assertion that actually tests the spacer arithmetic -
                // rows tiling each other is what a flex column does regardless.
                .check("nothing is reserved above the first row at the top", ctx ->
                        leadingSpacer(ctx) < 0.5f)

                .step("scroll the ragged list to the middle", ctx ->
                        ragged(ctx).verticalScroller.setValue(0.5f))
                .frames(5)
                .check("the ragged window moved", ctx ->
                        ctx.query().select(".ragged-row-0").optional().isEmpty())
                .check("and now reserves space for the rows above it", ctx ->
                        leadingSpacer(ctx) > 0f && trailingSpacer(ctx) > 0f)
                .check("and its rows are still contiguous after measuring new ones", ctx ->
                        raggedRowsAreContiguous(ctx))

                .step("scroll the ragged list to the end", ctx ->
                        ragged(ctx).verticalScroller.setValue(1f))
                .frames(5)
                .check("the last ragged row is reachable", ctx ->
                        ctx.query().select(".ragged-row-399").optional().isPresent())
                // The mirror of the check at the top, and the one that catches a trailing spacer
                // that is too big: the list would then scroll past its own last row.
                .check("and nothing is reserved below it", ctx -> trailingSpacer(ctx) < 0.5f)
                .check("and the rows are contiguous at the bottom too", ctx ->
                        raggedRowsAreContiguous(ctx))

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

    private static ScrollerView ragged(TestContext ctx) {
        return ctx.query().select(".ragged-list").one().as(ScrollerView.class);
    }

    private static float raggedContentHeight(TestContext ctx) {
        float height = 0f;
        for (var child : ragged(ctx).viewContainer.getChildren()) {
            height += child.getSizeHeight();
        }
        return height;
    }

    /** The empty element standing in for everything above the mounted window. */
    private static float leadingSpacer(TestContext ctx) {
        var children = ragged(ctx).viewContainer.getChildren();
        return children.isEmpty() ? -1f : children.get(0).getSizeHeight();
    }

    /** And the one standing in for everything below it. */
    private static float trailingSpacer(TestContext ctx) {
        var children = ragged(ctx).viewContainer.getChildren();
        return children.isEmpty() ? -1f : children.get(children.size() - 1).getSizeHeight();
    }

    private static List<Float> raggedHeights(TestContext ctx) {
        return ctx.query().select(".ragged-row").list().stream()
                .map(ref -> ref.bounds().height()).toList();
    }

    /**
     * Whether the mounted ragged rows tile their container without overlapping or leaving a gap.
     *
     * <p>Walks the container's own children rather than the query result, because a query returns
     * registration order and these have to be checked in document order. Each measured row sits
     * inside a wrapper, so the wrappers are what tile - the rows are their only children.
     */
    private static boolean raggedRowsAreContiguous(TestContext ctx) {
        Float previousBottom = null;
        for (var child : ragged(ctx).viewContainer.getChildren()) {
            if (child.getSizeHeight() <= 0f) continue;
            float top = child.getPositionY();
            if (previousBottom != null && Math.abs(top - previousBottom) > 0.5f) {
                return false;
            }
            previousBottom = top + child.getSizeHeight();
        }
        return previousBottom != null;
    }

    private static List<String> rowTexts(TestContext ctx) {
        return ctx.query().select(".lazy-row").list().stream().map(ref -> Texts.of(ref.as(
                com.lowdragmc.lowdraglib2.gui.ui.UIElement.class))).toList();
    }
}
