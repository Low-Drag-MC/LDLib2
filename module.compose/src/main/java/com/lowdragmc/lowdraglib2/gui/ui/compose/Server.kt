package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEvent
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import net.minecraft.nbt.CompoundTag

/**
 * Sends things to the server from inside a composition.
 *
 * <p>**Why this exists, and the one rule that makes server-backed compose work.**
 *
 * <p>`UISyncManager` addresses sync values and RPC events by *registration order* — the sender writes
 * an index and the receiver looks that index up in its own list. Both sides build their UI by running
 * the same `createUI`, so the lists line up. A composition breaks that: it runs on the client only,
 * and it creates and destroys elements as state changes, so anything it registered would be numbered
 * differently on the two sides, or not exist on the server at all. That is why the ordinary
 * per-widget tools — `bindDataSource`, `setOnServerClick`, `addServerEventListener` — must not be
 * reached for from inside a composable: each of them registers, and registering from a composition is
 * how the two sides stop agreeing.
 *
 * <p>The rule that follows is simple: **anything that registers goes in `createUI`, outside the
 * composition; the composition only ever *uses* what was registered there.** Which is not a
 * restriction on how dynamic the UI can be, because one registration can carry any amount of dynamic
 * content:
 *
 * <ul>
 * <li><b>Server → client:</b> one sync value carrying the whole model. A list of fifty rows and a
 *     list of none are the same single slot, so the numbering never moves.</li>
 * <li><b>Client → server:</b> messages, which this class sends. They are addressed by *name*, and
 *     every name shares one RPC event underneath ({@code UIElement.getOrCreateMessageRPC}) — so a
 *     composition can send any message it likes without registering anything.</li>
 * </ul>
 *
 * ```kotlin
 * // in createUI, on both sides
 * val host = ComposeElement { Screen() }
 * host.onMessage("remove") { tag -> rows = rows - tag.getString("id") }
 * root.addChild(host)
 *
 * // in the composition, client only
 * val server = rememberServerMessenger()
 * Button(onClick = { server.send("remove") { putString("id", row.id) } })
 * ```
 *
 * <p>The handler must be registered on the same element the composition sends from — the one
 * [LocalUIElement] resolves to, which is the [ComposeElement] hosting the composition. That is what
 * puts the shared message RPC at the same index on both sides.
 */
@Stable
class ServerMessenger internal constructor(private val host: UIElement) {

    /** Sends [name] with no payload. */
    fun send(name: String) = host.sendMessage(name)

    /** Sends [name] carrying [data]. */
    fun send(name: String, data: CompoundTag) = host.sendMessage(name, data)

    /** Sends [name], building its payload inline. */
    inline fun send(name: String, build: CompoundTag.() -> Unit) =
        send(name, CompoundTag().apply(build))

    /**
     * Calls a typed [RPCEvent] instead of sending a message.
     *
     * <p>For arguments a `CompoundTag` would only be a container for. Same rule as everything else
     * here: [event] must have been registered in `createUI`, on both sides, in a fixed order.
     */
    fun call(event: RPCEvent, vararg args: Any?) = host.sendEvent(event, *args)
}

/**
 * The messenger for the composition's host element.
 *
 * <p>Safe to call anywhere in the tree, including inside a [Subcompose] — the host travels with the
 * composition rather than with the element being emitted.
 */
@Composable
fun rememberServerMessenger(): ServerMessenger {
    val host = LocalUIElement.current
    return remember(host) { ServerMessenger(host) }
}
