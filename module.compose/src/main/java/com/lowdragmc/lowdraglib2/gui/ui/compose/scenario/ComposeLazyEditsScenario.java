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

/**
 * Lazy containers whose data changes while they are on screen.
 *
 * <p>The scroll position is a fraction of the content, so without anchoring every change in length
 * moves the content under the reader: a hundred rows appended below put them on a different row, and
 * twenty prepended above push everything down. The list has to stay on the row that was showing.
 */
@LDLRegisterClient(name = "compose_lazy_edits", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeLazyEditsScenario implements UIScenario {

    private String topEntry;

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

                // A grid fed from a snapshot list added to in place: the same list instance, so a
                // change has to be noticed by its size.
                .checkExists(".le-cell-g9")
                .checkNotExists(".le-cell-g13")
                .step("add four cells", ctx -> press(ctx, ".le-grow"))
                .frames(3)
                .checkExists(".le-cell-g13")

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
        return ctx.query().select(".le-feed").one().as(ScrollerView.class);
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
        var event = UIEvent.create(UIEvents.MOUSE_DOWN);
        event.target = ctx.query().select(selector).one().as(UIElement.class);
        event.button = 0;
        UIEventDispatcher.dispatchEvent(event);
    }
}
