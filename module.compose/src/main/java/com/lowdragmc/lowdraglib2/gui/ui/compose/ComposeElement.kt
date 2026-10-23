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
 * A [UIElement] whose children are produced by a composition rather than assembled by hand.
 *
 * <p>This is the seam in both directions. Existing Java or DSL code can drop a declarative subtree
 * into an ordinary tree with `parent.addChild(ComposeElement { MyScreen() })`, and
 * [composeUI] builds a whole UI out of one.
 *
 * <p>The composition belongs to the element, not to the [ModularUI] it currently lives in. That
 * distinction is load-bearing: minimising an editor window keeps the element tree but re-hosts it in
 * a fresh `ModularUI`, and anything keyed on the old host is abandoned at that point. Only the
 * per-frame registration follows the host around; state built up by `remember` survives the move.
 *
 * <p>Closing the screen is different: the composition goes, and `remember` state with it. Give the
 * element a [stateKey] for `rememberSaveable` state to come back the next time it is built.
 *
 * <p>Children the composition did not create are left alone: `addChild` on this element works, and
 * those children sit after the composed ones.
 *
 * <p>Don't build one per frame — in a tooltip component, say. A composition only lets go of the
 * global snapshot system when the element is removed from its UI, and a tooltip's UI never removes
 * anything; it is just dropped.
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

    init {
        addEventListener(UIEvents.MUI_CHANGED, UIEventListener { rebind() })
        // On both sides, at construction. The composition only exists on the client, so a message
        // sent from it would otherwise be what registers the message RPC - client-side only, and
        // then every RPC after it is out of step with the server's.
        getOrCreateMessageRPC()
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
     * the content; layout and styling are no-ops there anyway ([UIElement.layout], [UIElement.style]),
     * and the server's half of a menu gets its data through the ordinary binding path.
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
     * Tears the composition down when the element is finished with, which is what `onRemoved` means
     * everywhere else in LDLib2 — `Scene`, `Switch` and `Toggle` all release their resources here.
     *
     * <p>Disposing here is not optional. A running `Recomposer` registers a *global* snapshot apply
     * observer and only releases it when it is cancelled, so a composition nobody disposed stays
     * reachable from that global list forever — and with it the applier, this element, and the entire
     * subtree it built.
     *
     * <p>Not terminal: a menu screen calls this when it is covered by a recipe viewer and then
     * re-initialises the same tree when it comes back, which rebuilds the composition from
     * [initScreen]. Re-hosting does not come through here at all: minimising an editor window
     * re-points the same tree at a fresh `ModularUI`, firing `MUI_CHANGED` without `onRemoved`.
     */
    override fun onRemoved() {
        // Before super, not after: disposing clears the composed children through the applier, so
        // running it first leaves super's own walk with only the children it should still visit.
        try {
            teardown()
        } catch (e: Throwable) {
            // Swallowed: this runs in the middle of UIElement#removeChild, and an exception escaping
            // here would leave the element half-removed - out of its parent, still in the UI.
            LDLib2.LOGGER.error("Error while disposing a Compose composition", e)
        }
        super.onRemoved()
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
            // Before disposing: the values are collected from the providers each rememberSaveable
            // registered, and disposing the composition unregisters them.
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
     * <p>`boundUI` means "the UI we are registered with", not "the UI we last saw" — the difference
     * matters when [setContent] is called on an element that is already attached. Skipping the work
     * because the host looked unchanged would leave a composition that never receives a frame, and
     * so never recomposes, with nothing to indicate why.
     */
    private fun rebind() {
        if (LDLib2.isServer()) return
        val mui = modularUI
        // Torn down by onRemoved and now attached again. Rebuilding from the content we were handed
        // beats leaving a silently empty element behind; `remember` state does not survive, because
        // the composition that held it is gone — `rememberSaveable` does, given a stateKey.
        if (host == null && mui != null && !failed) content?.let { startComposition(it) }
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
