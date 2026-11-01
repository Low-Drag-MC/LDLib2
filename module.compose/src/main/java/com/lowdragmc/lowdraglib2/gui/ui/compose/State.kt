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
 * The element hosting this composition: the way to reach the surrounding
 * [com.lowdragmc.lowdraglib2.gui.ui.ModularUI], its sync manager and its player from a composable.
 */
val LocalUIElement = staticCompositionLocalOf<UIElement> {
    error("No host UIElement in this composition. Compose trees must be created via ComposeElement.")
}

/**
 * Follows an LDLib2 data source as Compose state, so anything reading it recomposes when it changes.
 *
 * <p>Server-synced values arrive through the existing binding machinery, built outside the
 * composition so the number and order of sync slots is fixed; this turns their arrival into a
 * recomposition:
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
 * element's 20 Hz tick, so they update at tick rate, not frame rate. Each call ticks the source it
 * was given, so several calls on one polling source tick it several times a tick; collect it once
 * and pass the state down.
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
 * <p>Installs the state as the binding's *remote* (client-side) data source — the slot
 * [IBindable.bind] would otherwise fill with the widget. Incoming server values land in the state;
 * writes to it are picked up by the binding on the next tick and sent back.
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
 * <p>Use it for a binding that is *not* also bound to a widget: both would claim the same data
 * source, and the second one wins.
 *
 * <p>Create the binding outside the composition: sync slots are assigned by registration order, and
 * recomposition must not be able to reorder them.
 */
@Composable
fun <T> IBinding<T>.asMutableState(): MutableState<T> {
    val state = remember(this) { mutableStateOf(syncValue.value) }
    DisposableEffect(this) {
        val source = StateDataSource(state, remoteDataSource)
        setRemoteDataSource(source)
        onDispose {
            source.active = false
            // Only if still ours: another composition following this binding may have taken over since.
            if (remoteDataSource === source) setRemoteDataSource(source.restorable() ?: IDataSource.empty())
        }
    }
    return state
}

/**
 * A binding's remote data source backed by Compose state. Chains to the one it replaced, so when two
 * compositions leave out of order the binding is never handed back to a state already gone.
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

/** Writes go to the [TrackData]; [backing] gets them back through the listener. */
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
