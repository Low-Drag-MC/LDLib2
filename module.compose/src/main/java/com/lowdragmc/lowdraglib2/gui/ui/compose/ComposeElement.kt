package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.lowdragmc.lowdraglib2.LDLib2
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import java.util.function.LongConsumer

/**
 * A [UIElement] whose children are produced by a composition rather than assembled by hand. Drop one
 * into an ordinary tree with `parent.addChild(ComposeElement { MyScreen() })`, or build a whole UI
 * out of one with [composeUI].
 *
 * <p>The composition belongs to the element, not to the [ModularUI] it currently lives in: when the
 * tree is re-hosted in a fresh `ModularUI` (minimising an editor window does this), `remember` state
 * survives and only the per-frame registration moves.
 *
 * <p>Closing the screen is different: the composition goes, and `remember` state with it. Give the
 * element a [stateKey] for `rememberSaveable` state to come back the next time it is built.
 *
 * <p>Children the composition did not create are left alone: `addChild` on this element works. The
 * composed children are kept together, in composition order.
 *
 * <p>One built inside a composition — `remember { ComposeElement { ... } }` — exists on the client
 * only, so it registers nothing; [rememberServerMessenger] inside it sends through the nearest
 * enclosing one that was built in `createUI`.
 *
 * <p>Don't build one per frame — in a tooltip component, say. A composition only lets go of the
 * global snapshot system when the element is removed from its UI, and a tooltip's UI is just dropped.
 */
class ComposeElement() : UIElement() {
    /**
     * Names this composition's `rememberSaveable` state, so it is kept in [ComposeSavedState] when the
     * element is torn down and handed back when an element with the same key is built. Pick something
     * that identifies the screen *and* what it shows — `"mymod:furnace@" + pos` rather than
     * `"mymod:furnace"`, or every furnace shares one set of state.
     *
     * <p>Read when the composition starts and when it is torn down, so set it before [setContent];
     * the constructor taking one does. Null, the default, keeps nothing across a close.
     */
    var stateKey: String? = null

    private var host: ComposeHost? = null
    private var boundUI: ModularUI? = null
    private var content: (@Composable () -> Unit)? = null

    /** Set when the content threw while composing, so it is not retried on every screen resize. */
    private var failed = false

    /** Stable instance, so unregistering from the previous host actually finds it. */
    private val frameCallback = LongConsumer { nanos -> host?.onFrame(nanos) }

    /** Built outside any composition, so it exists on both sides and can carry their messages. */
    internal val carriesMessages = !ComposeHost.isRunning

    init {
        // Only rebinds: the children are not registered with the new UI yet when this fires, so
        // rebuilding into it waits for onAdded / initScreen.
        addEventListener(UIEvents.MUI_CHANGED, UIEventListener { rebind(rebuild = false) })
        // At construction, on both sides: registered later from a composition it would be client-only
        // and put every RPC after it out of step with the server's.
        if (carriesMessages) getOrCreateMessageRPC()
    }

    constructor(content: @Composable () -> Unit) : this() {
        setContent(content)
    }

    constructor(stateKey: String?, content: @Composable () -> Unit) : this() {
        this.stateKey = stateKey
        setContent(content)
    }

    /**
     * Sets what this element renders. The first composition runs immediately, so the children exist
     * by the time this returns and the element can be laid out in the same frame it was built.
     *
     * <p>Composes on the client's render thread only. On a dedicated server, and on the integrated
     * server's own thread in singleplayer — where a menu's `createUI` also runs — this only records
     * the content; the server's half of a menu gets its data through the ordinary binding path.
     *
     * <p>If the content throws while composing, the error is logged and this element stays empty
     * until the next call.
     */
    fun setContent(content: @Composable () -> Unit) {
        this.content = content
        failed = false
        if (LDLib2.isServer()) return
        startComposition(content)
        rebind()
    }

    /**
     * Tears the composition down. Required: a running `Recomposer` holds a *global* snapshot apply
     * observer until it is cancelled, which would keep this element and its whole subtree reachable.
     *
     * <p>Not terminal: a menu screen calls this when it is covered by a recipe viewer and then
     * re-initialises the same tree when it comes back, which rebuilds the composition from
     * [initScreen]. Re-hosting in a fresh `ModularUI` fires `MUI_CHANGED` and does not come through here.
     */
    override fun onRemoved() {
        // Before super: disposing removes the composed children, so super's walk visits only what remains.
        try {
            teardown()
        } catch (e: Throwable) {
            // Swallowed: escaping mid-removeChild would leave the element out of its parent but still in the UI.
            LDLib2.LOGGER.error("Error while disposing a Compose composition", e)
        }
        super.onRemoved()
    }

    override fun onAdded() {
        super.onAdded()
        rebind()
    }

    /** Runs whenever the screen (re)initialises - including a menu screen coming back into view. */
    override fun initScreen(screenWidth: Int, screenHeight: Int) {
        super.initScreen(screenWidth, screenHeight)
        rebind()
    }

    /**
     * Tears the composition down for good, running every `DisposableEffect`'s cleanup and saving its
     * `rememberSaveable` state if it has a [stateKey]. Nothing rebuilds it afterwards unless
     * [setContent] is called again.
     */
    fun dispose() {
        content = null
        teardown()
    }

    private fun teardown() {
        boundUI?.removeFrameCallback(frameCallback)
        boundUI = null
        val host = this.host ?: return
        this.host = null
        try {
            // Before disposing, which unregisters the rememberSaveable providers the values come from.
            stateKey?.let { ComposeSavedState.save(it, host.saveState()) }
        } finally {
            host.dispose()
        }
    }

    private fun startComposition(content: @Composable () -> Unit) {
        val host = this.host ?: ComposeHost(this, stateKey?.let(ComposeSavedState::restore))
        try {
            host.setContent {
                CompositionLocalProvider(LocalUIElement provides this) { content() }
            }
            this.host = host
        } catch (e: Throwable) {
            LDLib2.LOGGER.error("Compose content failed while composing; this element stays empty", e)
            failed = true
            this.host = null
            try {
                host.dispose()
            } catch (cleanup: Throwable) {
                e.addSuppressed(cleanup)
            }
        }
    }

    /**
     * Brings the composition in line with whichever [ModularUI] currently hosts this element: rebuilds
     * it if it was torn down, starts it following state, and points the frame callback at that UI.
     *
     * <p>`boundUI` is the UI the callback is registered with, not the last one seen; otherwise
     * [setContent] on an attached element would leave a composition that never receives a frame.
     */
    private fun rebind(rebuild: Boolean = true) {
        if (LDLib2.isServer()) return
        val mui = modularUI
        // Torn down by onRemoved and attached again: rebuild. Only `rememberSaveable` state survives.
        if (rebuild && host == null && mui != null && !failed) content?.let { startComposition(it) }
        val host = host
        if (host != null && mui != null) host.start()
        if (mui === boundUI) return
        boundUI?.removeFrameCallback(frameCallback)
        boundUI = null
        // A detached element keeps its composition; it just stops being ticked until re-hosted.
        if (host != null && mui != null) {
            mui.addFrameCallback(frameCallback)
            boundUI = mui
        }
    }
}
