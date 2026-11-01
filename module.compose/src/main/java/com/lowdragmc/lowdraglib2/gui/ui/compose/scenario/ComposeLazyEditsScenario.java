package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestComposeLazyEdits;
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
import com.lowdragmc.lowdraglib2.uitest.target.Texts;

/** Lazy containers whose data changes while they are on screen, which must stay on the row that was showing. */
@LDLRegisterClient(name = "compose_lazy_edits", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeLazyEditsScenario implements UIScenario {

    private String topEntry;
    private String nextEntry;
    private UIElement anchored;
    private float anchoredY;
    private UIElement savedCellRow;
    private String firstCell;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("compose").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("compose lazy edits demo", ctx -> new TestComposeLazyEdits().createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement(".le-root")
                .frames(3)

                .step("scroll the feed to the middle", ctx -> feed(ctx).verticalScroller.setValue(0.5f))
                .frames(5)
                .step("note the row at the top", ctx -> topEntry = entryAtTop(ctx))
                .check("some middle row is at the top", ctx -> topEntry != null && !"entry 0".equals(topEntry))

                .step("append a hundred rows below", ctx -> press(ctx, ".le-append"))
                .frames(5)
                .check("the same row is still at the top", ctx -> topEntry.equals(entryAtTop(ctx)))

                .step("prepend twenty rows above", ctx -> press(ctx, ".le-prepend"))
                .frames(5)
                .check("and still after rows went in above it", ctx -> topEntry.equals(entryAtTop(ctx)))

                // The row at the top deleted: the one under it takes its place.
                .step("note the row under the top one", ctx -> {
                    var below = entryAt(ctx, ".le-feed", ".le-entry", 15f);
                    nextEntry = below == null ? null : Texts.of(below);
                })
                .step("delete the top row", ctx -> press(ctx, entryAt(ctx, ".le-feed", ".le-entry", 1f)))
                .frames(5)
                .check("the next row is at the top", ctx -> nextEntry != null && nextEntry.equals(entryAtTop(ctx)))

                // One row inserted above ten thousand is a sliver of the scroll range, but still a row.
                .step("scroll the long list to the middle", ctx -> view(ctx, ".le-long").verticalScroller.setValue(0.5f))
                .frames(5)
                .step("note its top row", ctx -> noteAnchor(ctx, ".le-long", ".le-long-entry"))
                .step("insert one row at the front", ctx -> press(ctx, ".le-long-prepend"))
                .frames(5)
                .check("the top row has not moved", ctx -> anchorHeld(ctx, ".le-long", ".le-long-entry"))

                // Rows on screen changing height keep the top row where it was.
                .step("scroll the resizing list to the middle", ctx -> view(ctx, ".le-var").verticalScroller.setValue(0.5f))
                .frames(8)
                .step("note its top row", ctx -> noteAnchor(ctx, ".le-var", ".le-var-entry"))
                .step("resize every third row", ctx -> press(ctx, ".le-var-resize"))
                .frames(8)
                .check("the top row has not moved", ctx -> anchorHeld(ctx, ".le-var", ".le-var-entry"))

                // The ordinary wheel scrolls a horizontal list.
                .step("wheel over the strip", ctx -> {
                    var event = UIEvent.create(UIEvents.MOUSE_WHEEL);
                    event.target = view(ctx, ".le-strip").viewPort;
                    event.deltaY = -1;
                    UIEventDispatcher.dispatchEvent(event);
                })
                .frames(3)
                .check("it scrolled sideways", ctx -> view(ctx, ".le-strip").horizontalScroller.getValue() > 0f)

                // A snapshot list added to in place: the same instance, so the change is only visible in its size.
                .checkExists(".le-cell-g9")
                .checkNotExists(".le-cell-g13")
                .step("add four cells", ctx -> press(ctx, ".le-grow"))
                .frames(3)
                .checkExists(".le-cell-g13")

                // A cell an insertion moves to another row is rebuilt there.
                .step("switch a cell on", ctx -> press(ctx, ".le-sg-s3"))
                .frames(3)
                .checkText(".le-sg-s3", "s3 on")
                .step("remember its row", ctx -> savedCellRow = row(element(ctx, ".le-sg-s3")))
                .step("insert a cell at the front", ctx -> press(ctx, ".le-sg-insert"))
                .frames(3)
                .check("it moved to the next row", ctx -> row(element(ctx, ".le-sg-s3")) != savedCellRow)
                .step("switch it on again", ctx -> press(ctx, ".le-sg-s3"))
                .frames(3)
                .checkText(".le-sg-s3", "s3 on")
                // A deleted cell's saved state goes with it.
                .step("delete it", ctx -> press(ctx, ".le-sg-drop"))
                .frames(3)
                .checkNotExists(".le-sg-s3")
                .step("and bring it back", ctx -> press(ctx, ".le-sg-restore"))
                .frames(3)
                .checkText(".le-sg-s3", "s3")

                // A new column count keeps the cells that were showing on screen.
                .step("scroll the grid to the middle", ctx -> view(ctx, ".le-cgrid").verticalScroller.setValue(0.5f))
                .frames(5)
                .step("note the first cell showing", ctx -> firstCell = firstVisibleCell(ctx))
                .step("halve the columns", ctx -> press(ctx, ".le-cg-columns"))
                .frames(5)
                .check("that cell is still showing", ctx -> firstCell != null && isShowing(ctx, ".le-cgrid", ".le-cg-" + firstCell))

                // Repeated keys: rendered, and the composition carries on.
                .check("every row with a repeated key is there", ctx -> ctx.query().select(".le-dupe").count() == 3)
                .step("add another repeat", ctx -> press(ctx, ".le-dupe-more"))
                .frames(3)
                .check("still all there", ctx -> ctx.query().select(".le-dupe").count() == 4)
                .step("and the composition is still alive", ctx -> press(ctx, ".le-grow"))
                .frames(3)
                .checkExists(".le-cell-g17")

                .closeScreen();
    }

    private static ScrollerView feed(TestContext ctx) {
        return view(ctx, ".le-feed");
    }

    private static ScrollerView view(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().as(ScrollerView.class);
    }

    /** The entry of a list covering a point {@code dy} below the top of its viewport. */
    private static UIElement entryAt(TestContext ctx, String list, String entries, float dy) {
        float at = view(ctx, list).viewPort.getPositionY() + dy;
        for (var ref : ctx.query().select(entries).list()) {
            var element = ref.as(UIElement.class);
            float y = element.getPositionY();
            if (y <= at && at < y + element.getSizeHeight()) return element;
        }
        return null;
    }

    private void noteAnchor(TestContext ctx, String list, String entries) {
        anchored = entryAt(ctx, list, entries, 1f);
        anchoredY = anchored == null ? Float.NaN : anchored.getPositionY();
    }

    /** The noted row is still the top one, and within a pixel of where it was. */
    private boolean anchorHeld(TestContext ctx, String list, String entries) {
        if (anchored == null) return false;
        var top = entryAt(ctx, list, entries, 1f);
        return top != null && Texts.of(top).equals(Texts.of(anchored))
                && Math.abs(top.getPositionY() - anchoredY) < 1f;
    }

    /** The grid row a cell sits in: cell text, then its cell box, then the row. */
    private static UIElement row(UIElement cell) {
        var box = cell.getParent();
        return box == null ? null : box.getParent();
    }

    /** The name of the top-left cell of the columns grid that is on screen. */
    private static String firstVisibleCell(TestContext ctx) {
        var viewport = view(ctx, ".le-cgrid").viewPort;
        float top = viewport.getPositionY() + 6f;
        UIElement best = null;
        for (var ref : ctx.query().select(".le-cg").list()) {
            var cell = ref.as(UIElement.class);
            float y = cell.getPositionY();
            if (y <= top && top < y + cell.getSizeHeight()
                    && (best == null || cell.getPositionX() < best.getPositionX())) {
                best = cell;
            }
        }
        return best == null ? null : Texts.of(best);
    }

    private static boolean isShowing(TestContext ctx, String list, String selector) {
        var viewport = view(ctx, list).viewPort;
        var found = ctx.query().select(selector).optional();
        if (found.isEmpty()) return false;
        var element = found.get().as(UIElement.class);
        float top = viewport.getPositionY();
        float bottom = top + viewport.getSizeHeight();
        float y = element.getPositionY();
        return y + element.getSizeHeight() > top && y < bottom;
    }

    private static UIElement element(TestContext ctx, String selector) {
        return ctx.query().select(selector).one().as(UIElement.class);
    }

    /** The text of the entry covering the top edge of the feed's viewport. */
    private static String entryAtTop(TestContext ctx) {
        var viewport = feed(ctx).viewPort;
        float top = viewport.getPositionY() + 1f;
        for (var ref : ctx.query().select(".le-entry").list()) {
            var element = ref.as(UIElement.class);
            float y = element.getPositionY();
            if (y <= top && top < y + element.getSizeHeight()) {
                return Texts.of(element);
            }
        }
        return null;
    }

    /** Fires a left mouse-down straight at an element, which is what a Button acts on. */
    private static void press(TestContext ctx, String selector) {
        press(ctx, element(ctx, selector));
    }

    private static void press(TestContext ctx, UIElement target) {
        var event = UIEvent.create(UIEvents.MOUSE_DOWN);
        event.target = target;
        event.button = 0;
        UIEventDispatcher.dispatchEvent(event);
    }
}
