package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.ui.TestSyncHooks;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

/**
 * End-to-end coverage of the {@link com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder}
 * sync lifecycle hooks and of the initial data pack's framing, against {@code ui_sync_hooks}.
 *
 * <p>The UI opens through the real menu path — server {@code openMenu} → open-screen packet →
 * {@code ModularUIContainerScreen} — so the hooks fire off genuine {@code UISyncManager} traffic
 * rather than a hand-made call. Each direction is asserted on <b>both</b> sides: that the sending
 * side fired {@code onBeforeSync}/{@code onAfterSync} and the receiving side fired
 * {@code onSyncReceived} plus the side-specific variant, and just as importantly that the hooks
 * meant for the other side did <b>not</b> fire.
 */
@LDLRegisterClient(name = "sync_hooks", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SyncHookScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("sync").requiresWorld(true).guiScale(3);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("drop the holders from any previous run", ctx -> TestSyncHooks.CLIENT = null)
                .server("drop the server holder", sc -> TestSyncHooks.SERVER = null)

                .server("open ui_sync_hooks", sc ->
                        PlayerUIMenuType.openUI(sc.player(), LDLib2.id("ui_sync_hooks")))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .awaitElement("#root")
                .waitUntil("the client holder exists", ctx -> TestSyncHooks.CLIENT != null)
                .waitUntilServer("the server holder exists", sc -> TestSyncHooks.SERVER != null)
                .checkExists("#counter_label")
                .checkExists("#bump_button")
                .checkExists("#flag_switch")

                // A C2S-only binding must be left out of the initial data pack: the client refuses
                // it, and readSyncData throws before consuming the payload, so every entry after it
                // in the same pack misparses. These three probes are the canary.
                .checkEquals("probeA survived the initial pack", 7, ctx -> client().getProbeA())
                .checkEquals("probeB survived the initial pack", "hello", ctx -> client().getProbeB())
                .checkEquals("probeC survived the initial pack", 4242, ctx -> client().getProbeC())

                // The initial data pack rides the open-screen packet, so the receiving side must
                // already have fired before a single tick of periodic sync has happened.
                .check("the client received the initial counter",
                        ctx -> client().counterHooks.received.get() > 0)
                .check("onRemoteSyncReceived fired on the client",
                        ctx -> client().counterHooks.remoteReceived.get() > 0)
                .check("onServerSyncReceived did not fire on the client",
                        ctx -> client().counterHooks.serverReceived.get() == 0)
                .checkServer("the server sent the initial counter",
                        sc -> server().counterHooks.beforeSync.get() > 0
                                && server().counterHooks.afterSync.get() > 0)
                .checkServer("no receive hook fired on the server for the s2c counter",
                        sc -> server().counterHooks.received.get() == 0)
                .screenshot("01_opened")

                // s -> c. The click runs on the server, so the server is the sending side.
                .step("record the client counter tallies", ctx -> {
                    ctx.put("clientReceived", client().counterHooks.received.get());
                    ctx.put("clientRemoteReceived", client().counterHooks.remoteReceived.get());
                })
                .server("record the server counter tallies", sc -> {
                    sc.put("serverBefore", server().counterHooks.beforeSync.get());
                    sc.put("serverAfter", server().counterHooks.afterSync.get());
                })
                .click("#bump_button")
                .waitForSync("the bumped counter reaches the client",
                        sc -> server().getCounter(),
                        ctx -> client().getCounter())
                .check("the counter actually moved", ctx -> client().getCounter() > 0)
                .waitForTextContains("#counter_label", "counter: 1")
                .check("onSyncReceived fired again on the client",
                        ctx -> client().counterHooks.received.get() > ctx.<Integer>get("clientReceived"))
                .check("onRemoteSyncReceived fired again on the client",
                        ctx -> client().counterHooks.remoteReceived.get() > ctx.<Integer>get("clientRemoteReceived"))
                .checkServer("onBeforeSync fired again on the server",
                        sc -> server().counterHooks.beforeSync.get() > sc.<Integer>get("serverBefore"))
                .checkServer("onAfterSync fired again on the server",
                        sc -> server().counterHooks.afterSync.get() > sc.<Integer>get("serverAfter"))
                .screenshot("02_after_bump")

                // c -> s. Flipping the switch makes the client the sending side.
                .check("the flag starts false on the client", ctx -> !client().isFlag())
                .checkServer("the flag starts false on the server", sc -> !server().isFlag())
                .step("record the client flag tallies", ctx ->
                        ctx.put("clientFlagBefore", client().flagHooks.beforeSync.get()))
                .click("#flag_switch")
                // Split from the sync wait on purpose: if the click never toggled the widget, the
                // wait below would time out and read as "sync is broken".
                .waitUntil("the switch turned on", ctx ->
                        Boolean.TRUE.equals(((Switch) ctx.el("#flag_switch").element()).getValue()))
                .waitForSync("the flipped flag reaches the server",
                        sc -> server().isFlag(),
                        ctx -> client().isFlag())
                .check("the flag actually flipped", ctx -> client().isFlag())
                .check("onBeforeSync fired on the client",
                        ctx -> client().flagHooks.beforeSync.get() > ctx.<Integer>get("clientFlagBefore"))
                .checkServer("onSyncReceived fired on the server",
                        sc -> server().flagHooks.received.get() > 0)
                .checkServer("onServerSyncReceived fired on the server",
                        sc -> server().flagHooks.serverReceived.get() > 0)
                .checkServer("onRemoteSyncReceived did not fire on the server",
                        sc -> server().flagHooks.remoteReceived.get() == 0)
                .screenshot("03_after_flag")

                .closeScreen()
                .teardown("close the container", ctx -> ctx.requirePlayer().closeContainer());
    }

    private static TestSyncHooks client() {
        var holder = TestSyncHooks.CLIENT;
        if (holder == null) throw new IllegalStateException("The client never built a ui_sync_hooks holder");
        return holder;
    }

    private static TestSyncHooks server() {
        var holder = TestSyncHooks.SERVER;
        if (holder == null) throw new IllegalStateException("The server never built a ui_sync_hooks holder");
        return holder;
    }
}
