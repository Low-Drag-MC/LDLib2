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
 * Proves at startup that the Compose runtime this mod ships actually loaded.
 *
 * <p>Worth doing because of how the runtime gets here. It is not on anyone's classpath — it is
 * Jar-in-Jar'd inside this mod, six nested jars of it (and this mod is itself usually Jar-in-Jar'd
 * inside the mod that uses it), and NeoForge unpacks those at load time. In
 * development none of that happens: the dev run puts everything on a flat classpath, so a Jar-in-Jar
 * declaration that is wrong — a missing transitive, a version range that resolved to an alias stub
 * with no classes in it — is invisible right up until it reaches a player.
 *
 * <p>Without this the first sign of that would be a `NoClassDefFoundError` the first time someone
 * opened a screen, hours in, pointing at a class nobody has heard of. A snapshot write and read, and
 * one saveable-state registry — the two runtime jars — are enough to touch the parts that matter and
 * cost microseconds once.
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
            // Rethrown, not swallowed: a runtime that is not there cannot be worked around, and
            // failing at setup names the cause while the mod list is still on screen.
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
 * <p>Client only. The event class does not exist on a dedicated server, and the `value` filter is what
 * keeps FML from loading this object there.
 */
@EventBusSubscriber(modid = "ldlib2_compose", value = [Dist.CLIENT])
object ComposeClientEvents {

    @JvmStatic
    @SubscribeEvent
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        ComposeSavedState.clearAll()
    }
}
