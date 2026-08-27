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
 */
class ComposeElement() : UIElement() {
    private var host: ComposeHost? = null
    private var boundUI: ModularUI? = null

    /** Stable instance, so unregistering from the previous host actually finds it. */
    private val frameCallback = LongConsumer { nanos -> host?.onFrame(nanos) }

    init {
        addEventListener(UIEvents.MUI_CHANGED, UIEventListener { rebindFrameCallback() })
    }

    constructor(content: @Composable () -> Unit) : this() {
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
        val host = this.host ?: ComposeHost(this).also {
            this.host = it
            rebindFrameCallback()
        }
        host.setContent {
            CompositionLocalProvider(LocalUIElement provides this) { content() }
        }
    }

    /**
     * Tears the composition down, running every `DisposableEffect`'s cleanup.
     *
     * <p>Not called on removal from a parent, on purpose — see the class comment. Call it when the
     * element is genuinely finished with.
     */
    fun dispose() {
        boundUI?.removeFrameCallback(frameCallback)
        boundUI = null
        host?.dispose()
        host = null
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
