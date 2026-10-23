package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBinding
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataSource
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.TrackData
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.util.ITickable

/**
 * The element hosting this composition.
 *
 * <p>Mostly an implementation detail — [collectAsState] needs somewhere to hang a tick listener —
 * but it is also the way to reach the surrounding [com.lowdragmc.lowdraglib2.gui.ui.ModularUI],
 * its sync manager and its player from inside a composable.
 */
val LocalUIElement = staticCompositionLocalOf<UIElement> {
    error("No host UIElement in this composition. Compose trees must be created via ComposeElement.")
}

/**
 * Follows an LDLib2 data source as Compose state, so anything reading it recomposes when it changes.
 *
 * <p>This is the seam between the two worlds. Server-synced values arrive through the existing
 * binding machinery — which stays outside the composition, where the number and order of sync slots
 * is fixed — and this turns their arrival into a recomposition:
 *
 * ```kotlin
 * // built once, outside the composition, so its sync slot never moves
 * val progress = TrackData(0f)
 * element.addSyncValue(DataBindingBuilder.floatValS2C { machine.progress }
 *     .remoteSetter(progress::setValue).build().syncValue)
 *
 * // inside a composable
 * val value by progress.collectAsState()
 * ProgressText(value)
 * ```
 *
 * <p>Sources that poll rather than push ([ITickable], such as
 * [com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource]) are ticked off the host
 * element's 20 Hz tick, the same way [com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement]
 * does it — so they update at tick rate, not frame rate. Each call ticks the source it was given, so
 * several calls on the same polling source tick it several times a tick; collect it once and pass
 * the state down.
 */
@Composable
fun <T> IDataProvider<T>.collectAsState(): State<T> {
    val host = LocalUIElement.current
    val state = remember(this) { mutableStateOf(getValue()) }
    DisposableEffect(this, host) {
        val subscription = registerListener({ value -> state.value = value }, true)
        val source = this@collectAsState
        val tickListener = if (source is ITickable) {
            UIEventListener { source.tick() }.also { host.addEventListener(UIEvents.TICK, it) }
        } else {
            null
        }
        onDispose {
            subscription.unsubscribe()
            if (tickListener != null) host.removeEventListener(UIEvents.TICK, tickListener)
        }
    }
    return state
}

/**
 * Two-way view of a synced [IBinding] as Compose state.
 *
 * <p>This installs the state as the binding's *remote* (client-side) data source — the slot
 * [IBindable.bind] would otherwise fill with the widget itself. Incoming server values land in the
 * state and recompose whatever reads it; writes go into the state and are picked up by the binding's
 * value provider on the next tick and sent back.
 *
 * ```kotlin
 * // built once, outside the composition, so its sync slot never moves
 * private val enabled = DataBindingBuilder.create(machine::isEnabled, machine::setEnabled)
 *     .syncType(Boolean::class.java).build()
 *
 * // inside a composable
 * var on by enabled.asMutableState()
 * Switch(checked = on, onCheckedChange = { on = it })
 * ```
 *
 * <p>Use it for a binding that is *not* also bound to a widget — binding one to both means two
 * things claiming the same data source, and the second one wins. The usual shape is a standalone
 * binding registered on the host element, or the `BindableValue` placeholder pattern.
 *
 * <p>Nothing here changes how many sync slots exist or in what order, which is the constraint that
 * keeps this safe: slot ids are assigned by registration order and never reused, so the binding has
 * to be created outside the composition, where recomposition cannot reorder it.
 */
@Composable
fun <T> IBinding<T>.asMutableState(): MutableState<T> {
    val state = remember(this) { mutableStateOf(syncValue.value) }
    DisposableEffect(this) {
        val source = StateDataSource(state, remoteDataSource)
        setRemoteDataSource(source)
        onDispose {
            source.active = false
            // Only if it is still ours: two compositions can follow the same binding, and the one
            // that installed itself first may well be the first to go.
            if (remoteDataSource === source) setRemoteDataSource(source.restorable() ?: IDataSource.empty())
        }
    }
    return state
}

/**
 * A binding's remote data source backed by Compose state, remembering the one it replaced.
 *
 * <p>The chain matters when two compositions follow one binding and leave in the opposite order to
 * the one they came in: the one leaving last would otherwise hand the binding back to a state whose
 * composition is already gone, and every later server value would land nowhere.
 */
internal class StateDataSource<T>(
    private val state: MutableState<T>,
    private val previous: IDataSource<T>?,
) : IDataSource<T> {
    var active = true

    override fun getValue(): T = state.value

    override fun setValue(value: T?): IDataSource<T> {
        @Suppress("UNCHECKED_CAST")
        state.value = value as T
        return this
    }

    /** The newest source down the chain whose composition is still around. */
    fun restorable(): IDataSource<T>? {
        var candidate = previous
        while (candidate is StateDataSource<T> && !candidate.active) candidate = candidate.previous
        return candidate
    }
}

/**
 * Two-way view of a [TrackData] as Compose state: reads recompose, writes go back to the [TrackData]
 * and reach everything else bound to it.
 */
@Composable
fun <T> TrackData<T>.asMutableState(): MutableState<T> {
    val backing = remember(this) { mutableStateOf(value) }
    DisposableEffect(this) {
        val subscription = registerListener({ v -> backing.value = v }, true)
        onDispose { subscription.unsubscribe() }
    }
    return remember(this, backing) { TrackDataState(this, backing) }
}

/**
 * Writes go to the [TrackData], not to [backing]. The new value comes back through the listener
 * registered in [asMutableState], so there is one source of truth rather than two that have to be
 * kept in step.
 */
private class TrackDataState<T>(
    private val data: TrackData<T>,
    private val backing: MutableState<T>,
) : MutableState<T> {
    override var value: T
        get() = backing.value
        set(newValue) {
            data.value = newValue
        }

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { value = it }
}
