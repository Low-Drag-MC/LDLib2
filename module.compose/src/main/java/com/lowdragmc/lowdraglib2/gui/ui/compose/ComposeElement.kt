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
 */
class ComposeElement() : UIElement() {
    /**
     * Names this composition's `rememberSaveable` state, so it is kept in [ComposeSavedState] when the
     * element is torn down and handed back when an element with the same key is built. Pick something
     * that identifies the screen *and* what it shows — `"mymod:furnace@" + pos` rather than
     * `"mymod:furnace"`, or every furnace shares one set of state.
     *
     * <p>Read when the composition starts and when it is disposed, so set it before [setContent];
     * the constructor taking one does. Null, the default, keeps nothing across a close.
     */
    var stateKey: String? = null

    private var host: ComposeHost? = null
    private var boundUI: ModularUI? = null
    private var content: (@Composable () -> Unit)? = null

    /** Stable instance, so unregistering from the previous host actually finds it. */
    private val frameCallback = LongConsumer { nanos -> host?.onFrame(nanos) }

    init {
        addEventListener(UIEvents.MUI_CHANGED, UIEventListener { rebindFrameCallback() })
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
     * <p>No-op off the client: composition drives layout and styling, and both of those are already
     * no-ops on the server ([UIElement.layout], [UIElement.style]). A server-side menu still gets its
     * data through the ordinary binding path.
     */
    fun setContent(content: @Composable () -> Unit) {
        if (!LDLib2.isClient()) return
        this.content = content
        startComposition(content)
        rebindFrameCallback()
    }

    /**
     * Tears the composition down when the element is finished with, which is what `onRemoved` means
     * everywhere else in LDLib2 — `Scene`, `Switch` and `Toggle` all release their resources here.
     *
     * <p>Disposing here is not optional. A running `Recomposer` registers a *global* snapshot apply
     * observer and only releases it when it is cancelled, so a composition nobody disposed stays
     * reachable from that global list forever — and with it the applier, this element, and the entire
     * subtree it built. Every screen the player opened would be retained for the rest of the session.
     *
     * <p>Re-hosting does not come through here, which is the reason this is safe: minimising an editor
     * window re-points the same tree at a fresh `ModularUI` via `init`, firing `MUI_CHANGED` without
     * ever calling `onRemoved`. A genuine detach does call it, and [rebindFrameCallback] rebuilds the
     * composition if the element is later attached again.
     */
    override fun onRemoved() {
        // Before super, not after: disposing clears the composed children through the applier, so
        // running it first leaves super's own walk with an empty list. The other order would send
        // onRemoved to every child twice.
        dispose()
        super.onRemoved()
    }

    /**
     * Tears the composition down, running every `DisposableEffect`'s cleanup. Saves its
     * `rememberSaveable` state first, if it has a [stateKey].
     */
    fun dispose() {
        boundUI?.removeFrameCallback(frameCallback)
        boundUI = null
        host?.let { host ->
            // Before disposing: the values are collected from the providers each rememberSaveable
            // registered, and disposing the composition unregisters them.
            stateKey?.let { ComposeSavedState.save(it, host.saveState()) }
            host.dispose()
        }
        host = null
    }

    private fun startComposition(content: @Composable () -> Unit) {
        val host = this.host
            ?: ComposeHost(this, stateKey?.let(ComposeSavedState::restore)).also { this.host = it }
        host.setContent {
            CompositionLocalProvider(LocalUIElement provides this) { content() }
        }
    }

    /**
     * Points the frame callback at whichever [ModularUI] currently hosts this element.
     *
     * <p>`boundUI` means "the UI we are registered with", not "the UI we last saw" — the difference
     * matters when [setContent] is called on an element that is already attached. Skipping the work
     * because the host looked unchanged would leave a composition that never receives a frame, and
     * so never recomposes, with nothing to indicate why.
     */
    private fun rebindFrameCallback() {
        val mui = modularUI
        // Attached again after [onRemoved] tore the composition down. Rebuilding from the content we
        // were handed beats leaving a silently empty element behind; `remember` state does not
        // survive, because the composition that held it is gone — `rememberSaveable` does, given a
        // stateKey.
        if (host == null && mui != null) content?.let { startComposition(it) }
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
