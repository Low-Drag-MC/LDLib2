package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestComposeServer;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

import java.util.List;

/**
 * A composition whose element count is decided by the server, driven through the real menu path.
 *
 * <p>The UI opens the way a player's would — server {@code openMenu} → open-screen packet →
 * {@code ModularUIContainerScreen} — so the model arrives in a genuine initial data pack and the
 * buttons talk back over genuine RPC traffic. Nothing here is simulated client-side.
 *
 * <p>What is actually being tested is the thing that does not work naively. {@code UISyncManager}
 * addresses sync values and RPC events by registration order, and a composition creates and destroys
 * elements as state changes — so a UI that registered per row would have the two sides disagreeing
 * about what index means what, and the disagreement is silent: entries misparse and the rest of the
 * pack is lost. The demo registers one sync value and one shared message RPC, both outside the
 * composition, and the assertions below check the row count moving in both directions across that
 * boundary while the numbering stays put.
 */
@LDLRegisterClient(name = "compose_server", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeServerScenario implements UIScenario {

    /** The menu screen while something else covers it. */
    private Screen covered;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("compose", "sync").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("drop the holders from any previous run", ctx -> TestComposeServer.setCLIENT(null))
                .server("drop the server holder", sc -> TestComposeServer.setSERVER(null))

                .server("open the compose server ui", sc ->
                        PlayerUIMenuType.openUI(sc.player(), LDLib2.id("compose_server")))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .awaitElement("#root")
                .waitUntil("the client built its holder", ctx -> TestComposeServer.getCLIENT() != null)
                .waitUntilServer("the server built its holder", sc -> TestComposeServer.getSERVER() != null)

                // The model rides the initial data pack, so the rows are there before any tick of
                // periodic sync has happened.
                .waitUntil("the server's three rows reached the composition", ctx -> rows(ctx) == 3)
                .checkTextContains(".cs-count", "rows: 3")
                .checkExists(".cs-row-alpha")
                .checkExists(".cs-row-bravo")
                .checkExists(".cs-row-charlie")
                .screenshotElement("server-rows", "#root")

                // In singleplayer the server's half of the menu runs createUI on the integrated
                // server's thread, in the same JVM. A composition there would be built, never shown,
                // and never disposed - so its host must stay empty.
                .waitUntilServer("the server's host composed nothing", sc -> {
                    var holder = TestComposeServer.getSERVER();
                    return holder != null && holder.getHost().getChildren().isEmpty();
                })

                // Client -> server. The button sends a *named message*, which the server turns into a
                // mutation of the authoritative list; the list then syncs back and the composition
                // grows a row it was never told about directly.
                .click(".cs-add")
                .waitUntilServer("the server added a row", sc -> serverRows(sc) == 4)
                .waitUntil("and the extra row reached the composition", ctx -> rows(ctx) == 4)
                .checkTextContains(".cs-count", "rows: 4")
                .checkExists(".cs-row-row-1")

                // Removal names a row in the message payload, which is the part that would break if
                // every row had registered its own RPC event.
                // Pressed rather than clicked: this button removes the row it lives in, and the
                // builder's release step re-resolves the selector - which by then matches nothing.
                .step("remove alpha", ctx -> press(ctx, ".cs-remove-alpha"))
                .waitUntilServer("the server removed alpha", sc ->
                        !serverRowList(sc).contains("alpha"))
                .waitUntil("and it left the composition", ctx ->
                        ctx.query().select(".cs-row-alpha").optional().isEmpty())
                .check("the other rows are still there", ctx -> rows(ctx) == 3)
                .checkExists(".cs-row-bravo")
                .checkExists(".cs-row-row-1")

                // Several more rows, to show the count is genuinely unbounded rather than the three
                // the UI happened to start with.
                .click(".cs-add")
                .click(".cs-add")
                .click(".cs-add")
                .waitUntilServer("the server has six rows", sc -> serverRows(sc) == 6)
                .waitUntil("and so does the composition", ctx -> rows(ctx) == 6)
                .checkTextContains(".cs-count", "rows: 6")

                // Emptying it is the case that would leave stale elements behind if the composition
                // were not really driving the tree.
                .click(".cs-clear")
                .waitUntilServer("the server emptied the list", sc -> serverRows(sc) == 0)
                .waitUntil("and every row left the composition", ctx -> rows(ctx) == 0)
                .checkTextContains(".cs-count", "rows: 0")
                .check("no row elements are left over", ctx ->
                        ctx.query().select(".cs-row").count() == 0)

                // And back up from empty, which exercises the mount path rather than the unmount one.
                .click(".cs-add")
                .waitUntil("a row comes back", ctx -> rows(ctx) == 1)

                // What a recipe viewer does: put its own screen up, then hand the menu screen back.
                // The menu stays open throughout, but the screen's removed() tears the composition
                // down and its init() only re-initialises the existing tree - so this is the path
                // that would leave the screen blank if nothing rebuilt it.
                .step("cover the menu screen", ctx -> {
                    covered = ctx.mc().screen;
                    ctx.mc().setScreen(new ChatScreen(""));
                })
                .frames(3)
                .step("and hand it back", ctx -> ctx.mc().setScreen(covered))
                .awaitScreen(ModularUIContainerScreen.class)
                .waitUntil("the rows are back", ctx -> rows(ctx) == 1)
                .click(".cs-add")
                .waitUntil("and the composition still follows the server", ctx -> rows(ctx) == 2)

                .closeScreen();
    }

    /** How many rows the composition currently has mounted. */
    private static int rows(TestContext ctx) {
        return ctx.query().select(".cs-row").count();
    }

    /** Fires a left mouse-down straight at an element, which is what a Button acts on. */
    private static void press(TestContext ctx, String selector) {
        var event = UIEvent.create(UIEvents.MOUSE_DOWN);
        event.target = ctx.query().select(selector).one().as(UIElement.class);
        event.button = 0;
        UIEventDispatcher.dispatchEvent(event);
    }

    private static int serverRows(com.lowdragmc.lowdraglib2.uitest.ServerContext sc) {
        return serverRowList(sc).size();
    }

    private static List<String> serverRowList(com.lowdragmc.lowdraglib2.uitest.ServerContext sc) {
        var holder = TestComposeServer.getSERVER();
        return holder == null ? List.of() : holder.getServerRows();
    }
}
