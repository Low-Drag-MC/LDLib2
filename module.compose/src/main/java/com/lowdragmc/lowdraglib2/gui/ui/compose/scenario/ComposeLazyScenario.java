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

/** Proves the lazy list is actually lazy, and still behaves like a list. */
@LDLRegisterClient(name = "compose_lazy", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeLazyScenario implements UIScenario {

    /** Upper bound on mounted rows; generous, because the point is the order of magnitude. */
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

                // The horizontal variant: the row direction has to go on the content container, not
                // the ScrollerView, or the items stay stacked while the scrollbars go sideways.
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

                // Variable-height rows: a wrong spacer shows up as rows overlapping or a gap between them.
                .check("the ragged list is virtualised too", ctx -> {
                    var mounted = ctx.query().select(".ragged-row").count();
                    return mounted > 0 && mounted < SANE_MOUNTED_LIMIT;
                })
                .check("its rows really are different heights", ctx ->
                        ctx.query().select(".ragged-row").list().stream()
                                .map(ref -> ref.bounds().height()).distinct().count() > 1)
                .check("rows sit one after another with no overlap and no gap", ctx ->
                        contiguous(ragged(ctx), true))
                .check("the scrollbar spans the whole ragged list", ctx ->
                        contentExtent(ragged(ctx), true) > 400 * 10f)
                // This is what tests the spacer arithmetic; rows tiling is what a flex column does regardless.
                .check("nothing is reserved above the first row at the top", ctx ->
                        leadingSpacer(ragged(ctx), true) < 0.5f)

                .step("scroll the ragged list to the middle", ctx ->
                        ragged(ctx).verticalScroller.setValue(0.5f))
                .frames(5)
                .check("the ragged window moved", ctx ->
                        ctx.query().select(".ragged-row-0").optional().isEmpty())
                .check("and now reserves space for the rows above it", ctx ->
                        leadingSpacer(ragged(ctx), true) > 0f && trailingSpacer(ragged(ctx), true) > 0f)
                .check("and its rows are still contiguous after measuring new ones", ctx ->
                        contiguous(ragged(ctx), true))

                .step("scroll the ragged list to the end", ctx ->
                        ragged(ctx).verticalScroller.setValue(1f))
                .frames(5)
                .check("the last ragged row is reachable", ctx ->
                        ctx.query().select(".ragged-row-399").optional().isPresent())
                // Catches a trailing spacer that is too big, which would scroll past the last row.
                .check("and nothing is reserved below it", ctx -> trailingSpacer(ragged(ctx), true) < 0.5f)
                .check("and the rows are contiguous at the bottom too", ctx ->
                        contiguous(ragged(ctx), true))

                // The same measuring across; the end spacers show the arithmetic ran on the right axis.
                .check("the ragged strip is virtualised", ctx -> {
                    var mounted = ctx.query().select(".ragged-cell").count();
                    return mounted > 0 && mounted < SANE_MOUNTED_LIMIT;
                })
                .check("its cells really are different widths", ctx ->
                        ctx.query().select(".ragged-cell").list().stream()
                                .map(ref -> ref.bounds().width()).distinct().count() > 1)
                .check("cells sit side by side with no overlap and no gap", ctx ->
                        contiguous(strip(ctx), false))
                .check("nothing is reserved before the first cell", ctx ->
                        leadingSpacer(strip(ctx), false) < 0.5f)
                .check("the strip's content spans every cell", ctx ->
                        contentExtent(strip(ctx), false) > 300 * 12f)

                .step("scroll the strip to the middle", ctx -> strip(ctx).horizontalScroller.setValue(0.5f))
                .frames(5)
                .check("the strip's window moved", ctx ->
                        ctx.query().select(".ragged-cell-0").optional().isEmpty())
                .check("and reserves space on both sides of it", ctx ->
                        leadingSpacer(strip(ctx), false) > 0f && trailingSpacer(strip(ctx), false) > 0f)
                .check("and its cells are still contiguous", ctx -> contiguous(strip(ctx), false))

                .step("scroll the strip to the end", ctx -> strip(ctx).horizontalScroller.setValue(1f))
                .frames(5)
                .check("the last ragged cell is reachable", ctx ->
                        ctx.query().select(".ragged-cell-299").optional().isPresent())
                .check("and nothing is reserved after it", ctx -> trailingSpacer(strip(ctx), false) < 0.5f)

                // A grid with variable rows. Cell 0 is tall, so the other cells of the first row stretch to it.
                .check("the ragged grid is virtualised", ctx -> {
                    var mounted = ctx.query().select(".ragged-grid-cell").count();
                    return mounted > 0 && mounted < 6 * SANE_MOUNTED_LIMIT;
                })
                .check("its rows really are different heights", ctx ->
                        raggedGrid(ctx).viewContainer.getChildren().stream()
                                .map(UIElement::getSizeHeight).filter(h -> h > 0f).distinct().count() > 1)
                .check("a short cell stretches to the tallest in its row", ctx ->
                        cellSlot(ctx, ".ragged-grid-cell-1").getSizeHeight() > 20f)
                .check("grid rows sit one after another", ctx -> contiguous(raggedGrid(ctx), true))
                .step("scroll the grid to the end", ctx -> raggedGrid(ctx).verticalScroller.setValue(1f))
                .frames(5)
                .check("the last grid cell is reachable", ctx ->
                        ctx.query().select(".ragged-grid-cell-599").optional().isPresent())
                .check("and nothing is reserved below the last row", ctx ->
                        trailingSpacer(raggedGrid(ctx), true) < 0.5f)
                .check("and the grid rows are contiguous at the bottom", ctx -> contiguous(raggedGrid(ctx), true))
                .screenshotElement("lazy-ragged-end", ".__root__")

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

    private static ScrollerView strip(TestContext ctx) {
        return ctx.query().select(".ragged-strip").one().as(ScrollerView.class);
    }

    private static ScrollerView raggedGrid(TestContext ctx) {
        return ctx.query().select(".ragged-grid").one().as(ScrollerView.class);
    }

    /** The grid's own box around a cell's content - the element that stretches to the row. */
    private static UIElement cellSlot(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().as(UIElement.class).getParent();
    }

    private static float extent(UIElement element, boolean vertical) {
        return vertical ? element.getSizeHeight() : element.getSizeWidth();
    }

    /** Total extent of the scrolling content along its axis, spacers included. */
    private static float contentExtent(ScrollerView view, boolean vertical) {
        float total = 0f;
        for (var child : view.viewContainer.getChildren()) {
            total += extent(child, vertical);
        }
        return total;
    }

    private static float leadingSpacer(ScrollerView view, boolean vertical) {
        var children = view.viewContainer.getChildren();
        return children.isEmpty() ? -1f : extent(children.get(0), vertical);
    }

    private static float trailingSpacer(ScrollerView view, boolean vertical) {
        var children = view.viewContainer.getChildren();
        return children.isEmpty() ? -1f : extent(children.get(children.size() - 1), vertical);
    }

    /**
     * Whether the item wrappers tile the container along its axis without overlap or gap. Walks the
     * children rather than a query, which returns registration order instead of document order.
     */
    private static boolean contiguous(ScrollerView view, boolean vertical) {
        Float previousEnd = null;
        for (var child : view.viewContainer.getChildren()) {
            var size = extent(child, vertical);
            if (size <= 0f) continue;
            float start = vertical ? child.getPositionY() : child.getPositionX();
            if (previousEnd != null && Math.abs(start - previousEnd) > 0.5f) {
                return false;
            }
            previousEnd = start + size;
        }
        return previousEnd != null;
    }

    private static List<String> rowTexts(TestContext ctx) {
        return ctx.query().select(".lazy-row").list().stream().map(ref -> Texts.of(ref.as(
                com.lowdragmc.lowdraglib2.gui.ui.UIElement.class))).toList();
    }
}
