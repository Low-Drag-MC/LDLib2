package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.Snapshot
import com.lowdragmc.lowdraglib2.LDLib2
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent

/**
 * Proves at startup that the Compose runtime this mod Jar-in-Jars actually loaded. Dev runs use a flat
 * classpath, so a wrong Jar-in-Jar declaration (a missing transitive, a range resolving to an empty
 * alias stub) would otherwise first show as a `NoClassDefFoundError` when a player opens a screen.
 * A snapshot write and read plus one saveable-state registry touch both runtime jars.
 */
@EventBusSubscriber(modid = "ldlib2_compose", bus = EventBusSubscriber.Bus.MOD)
object ComposeModule {

    @JvmStatic
    @SubscribeEvent
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        try {
            val probe = mutableStateOf(0)
            Snapshot.withMutableSnapshot { probe.value = 1 }
            check(probe.value == 1) { "snapshot write did not stick" }
            SaveableStateRegistry(null) { true }.performSave()
            LDLib2.LOGGER.info("[compose] runtime ready ({})", Snapshot::class.java.packageName)
        } catch (e: Throwable) {
            // Rethrown: failing at setup names the cause while the mod list is still on screen.
            throw IllegalStateException(
                "ldlib2-compose could not load the Compose runtime it ships with. Whatever jar " +
                    "brought ldlib2-compose in has to carry its -all jar (which embeds the runtime under " +
                    "META-INF/jarjar), not the plain one - check the mod that bundles it.",
                e,
            )
        }
    }
}

/**
 * Forgets saved screen state when the player leaves a world; see [ComposeSavedState].
 *
 * <p>Client only: the `value` filter keeps FML from loading this object on a dedicated server, where
 * the event class does not exist.
 */
@EventBusSubscriber(modid = "ldlib2_compose", value = [Dist.CLIENT])
object ComposeClientEvents {

    @JvmStatic
    @SubscribeEvent
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        ComposeSavedState.clearAll()
    }

    /** Again on the way in: a screen closed by the disconnect itself saves after LoggingOut has run. */
    @JvmStatic
    @SubscribeEvent
    fun onLoggingIn(event: ClientPlayerNetworkEvent.LoggingIn) {
        ComposeSavedState.clearAll()
    }
}
