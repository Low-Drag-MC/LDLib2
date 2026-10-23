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
 * <p>Sync values and RPC events are matched between the two sides by *registration order*, and a
 * composition runs on the client only, so nothing that registers — `bindDataSource`,
 * `setOnServerClick`, `addServerEventListener` — may be called from inside a composable. **Register
 * in `createUI`, outside the composition; the composition only uses what was registered there:**
 *
 * <ul>
 * <li><b>Server → client:</b> one sync value carrying the whole model, so the numbering never moves
 *     however many rows it holds.</li>
 * <li><b>Client → server:</b> messages, which this class sends. They are addressed by *name* over one
 *     shared RPC event, so sending any message registers nothing.</li>
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
 * <p>The handler must be registered on the [ComposeElement] built in `createUI` that hosts the
 * composition — the one messages go through, also from a [ComposeElement] nested inside it.
 */
@Stable
class ServerMessenger internal constructor(private val element: UIElement) {
    /** The nearest enclosing [ComposeElement] built outside a composition; see [ComposeElement]. */
    private val host: UIElement
        get() = generateSequence(element) { it.parent }
            .firstOrNull { it is ComposeElement && it.carriesMessages } ?: element

    /** Sends [name] with no payload. */
    fun send(name: String) = host.sendMessage(name)

    /** Sends [name] carrying [data]. */
    fun send(name: String, data: CompoundTag) = host.sendMessage(name, data)

    /** Sends [name], building its payload inline. */
    inline fun send(name: String, build: CompoundTag.() -> Unit) =
        send(name, CompoundTag().apply(build))

    /**
     * Calls a typed [RPCEvent] instead of sending a message. [event] must have been registered in
     * `createUI`, on both sides, in a fixed order.
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
