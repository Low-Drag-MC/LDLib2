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
 * A composition whose element count is decided by the server, driven through the real menu path, so
 * the model arrives in a genuine initial data pack and the buttons talk back over real RPC traffic.
 *
 * <p>{@code UISyncManager} addresses sync values and RPC events by registration order, so registering
 * per row would silently desync the two sides. The demo registers one sync value and one shared
 * message RPC outside the composition; the checks move the row count both ways across that boundary.
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

                // The model rides the initial data pack, so the rows are there before any periodic sync.
                .waitUntil("the server's three rows reached the composition", ctx -> rows(ctx) == 3)
                .checkTextContains(".cs-count", "rows: 3")
                .checkExists(".cs-row-alpha")
                .checkExists(".cs-row-bravo")
                .checkExists(".cs-row-charlie")
                .screenshotElement("server-rows", "#root")

                // In singleplayer the server's createUI runs in the same JVM; a composition there would
                // never be shown or disposed, so its host must stay empty.
                .waitUntilServer("the server's host composed nothing", sc -> {
                    var holder = TestComposeServer.getSERVER();
                    return holder != null && holder.getHost().getChildren().isEmpty();
                })

                // The fluid slot's widget registers an RPC in its constructor; composed, it must not, or
                // every later RPC shifts on the client only and the second composition's ping is lost.
                .checkExists(".cs-fluid")
                .click(".cs-ping")
                .waitUntilServer("a message sent from after the composed slot still arrives", sc -> {
                    var holder = TestComposeServer.getSERVER();
                    return holder != null && holder.getServerPings() == 1;
                })

                // Client -> server: a named message mutates the server's list, which syncs back as a row.
                .click(".cs-add")
                .waitUntilServer("the server added a row", sc -> serverRows(sc) == 4)
                .waitUntil("and the extra row reached the composition", ctx -> rows(ctx) == 4)
                .checkTextContains(".cs-count", "rows: 4")
                .checkExists(".cs-row-row-1")

                // Removal names its row in the message payload. Pressed rather than clicked: the button
                // removes its own row, so the release step's selector would match nothing.
                .step("remove alpha", ctx -> press(ctx, ".cs-remove-alpha"))
                .waitUntilServer("the server removed alpha", sc ->
                        !serverRowList(sc).contains("alpha"))
                .waitUntil("and it left the composition", ctx ->
                        ctx.query().select(".cs-row-alpha").optional().isEmpty())
                .check("the other rows are still there", ctx -> rows(ctx) == 3)
                .checkExists(".cs-row-bravo")
                .checkExists(".cs-row-row-1")

                // Several more rows than the three it started with.
                .click(".cs-add")
                .click(".cs-add")
                .click(".cs-add")
                .waitUntilServer("the server has six rows", sc -> serverRows(sc) == 6)
                .waitUntil("and so does the composition", ctx -> rows(ctx) == 6)
                .checkTextContains(".cs-count", "rows: 6")

                // Emptying it, which leaves stale elements behind unless the composition drives the tree.
                .click(".cs-clear")
                .waitUntilServer("the server emptied the list", sc -> serverRows(sc) == 0)
                .waitUntil("and every row left the composition", ctx -> rows(ctx) == 0)
                .checkTextContains(".cs-count", "rows: 0")
                .check("no row elements are left over", ctx ->
                        ctx.query().select(".cs-row").count() == 0)

                // And back up from empty, which exercises the mount path rather than the unmount one.
                .click(".cs-add")
                .waitUntil("a row comes back", ctx -> rows(ctx) == 1)

                // What a recipe viewer does: cover the menu screen, then hand it back. removed() tears
                // the composition down and init() only re-initialises the tree, so it must be rebuilt.
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
