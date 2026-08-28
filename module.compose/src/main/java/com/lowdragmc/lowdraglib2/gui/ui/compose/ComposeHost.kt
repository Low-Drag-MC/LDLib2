package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import com.lowdragmc.lowdraglib2.LDLib2
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Runs continuations when told to, instead of when they are submitted.
 *
 * <p>This is what keeps the whole Compose machine on the render thread. Recomposition, effects and
 * frame callbacks all funnel through here and only run inside [ComposeHost.onFrame], so nothing
 * Compose does can touch the UI tree from another thread — which matters, because none of LDLib2's
 * UI is thread-safe.
 */
private class RenderThreadDispatcher : CoroutineDispatcher() {
    private val queue = ArrayDeque<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.addLast(block)
    }

    fun drain() {
        // Not a for-loop over a snapshot: running a continuation commonly queues the next one, and
        // those have to come out in this same drain or the composition stalls for a frame.
        while (queue.isNotEmpty()) {
            val block = queue.removeFirst()
            try {
                block.run()
            } catch (e: Throwable) {
                LDLib2.LOGGER.error("Uncaught error while running a Compose continuation", e)
            }
        }
    }
}

/**
 * What to tell someone whose first coroutine dies with an `IllegalAccessError`.
 *
 * <p>kotlinx-coroutines ships a java agent (`Premain-Class` in its manifest) that rewrites
 * `kotlin.coroutines.jvm.internal.DebugProbesKt` to call into its own `DebugProbesImpl`. IntelliJ
 * attaches that agent by default whenever it sees coroutines on the classpath. The rewritten class
 * still belongs to the `kotlin.stdlib` module, which does not read `kotlinx.coroutines.core`, so
 * creating *any* coroutine throws — and the recomposer's is the first one Compose creates.
 *
 * <p>There is no fix on this side: `--add-reads` does not reach ModLauncher's module layer (tried),
 * the agent has no off switch of its own, and the read edge cannot be added from outside the module.
 * It also never happens in production, where nothing attaches the agent. So the useful thing to do
 * is name the cause instead of letting a JPMS message stand on its own.
 */
private const val COROUTINE_AGENT_ADVICE =
    "Compose could not start its recomposer coroutine. This is almost certainly the Kotlin " +
        "coroutines debug agent: IntelliJ attaches it automatically, it rewrites kotlin-stdlib's " +
        "DebugProbesKt to call into kotlinx-coroutines, and NeoForge's module layer forbids that. " +
        "Turn the coroutine agent off for this run (IDE settings, search for \"coroutine agent\"), " +
        "or launch with `gradlew runClient` instead. Nothing is wrong with the mod itself, and this " +
        "cannot happen outside a development environment."

/**
 * Owns one composition and drives it from LDLib2's render loop.
 *
 * <p>Compose's runtime is deliberately UI-toolkit agnostic — the two things it needs from a host are
 * somewhere to apply tree edits ([UIApplier]) and something to tell it when a frame happens. This
 * supplies both, so the recomposition engine, `remember`, the snapshot system and effects all work
 * exactly as they do on Android, against LDLib2's own element tree.
 *
 * <p>Call [onFrame] once per rendered frame; [com.lowdragmc.lowdraglib2.gui.ui.ModularUI.addFrameCallback]
 * is wired to do that.
 */
class ComposeHost(root: UIElement) {
    private val clock = BroadcastFrameClock()
    private val dispatcher = RenderThreadDispatcher()

    /**
     * Tracks the state that each node's `layout` / `style` blocks read, so those blocks re-run when
     * it changes. See [NodeObserver] for why the composition cannot do this by itself.
     */
    private val nodeObserver = NodeObserver()
    private val errorHandler = CoroutineExceptionHandler { _, e ->
        LDLib2.LOGGER.error("Compose recomposition failed; this composition is now stopped", e)
    }
    private val scope = CoroutineScope(dispatcher + clock + errorHandler)
    private val recomposer = Recomposer(scope.coroutineContext)
    private val composition = Composition(UIApplier(root), recomposer)

    var isDisposed: Boolean = false
        private set

    init {
        nodeObserver.start()
        try {
            scope.launch { recomposer.runRecomposeAndApplyChanges() }
        } catch (e: IllegalAccessError) {
            throw IllegalStateException(COROUTINE_AGENT_ADVICE, e)
        }
        // Let the recomposer reach its first suspension point, so it is already waiting on the clock
        // by the time the first frame arrives.
        dispatcher.drain()
    }

    /**
     * Sets — or replaces — what this composition renders. The initial composition runs synchronously,
     * so the element tree is fully built by the time this returns.
     */
    fun setContent(content: @Composable () -> Unit) {
        check(!isDisposed) { "This ComposeHost has been disposed" }
        composition.setContent {
            CompositionLocalProvider(LocalNodeObserver provides nodeObserver) { content() }
        }
    }

    /**
     * One frame: publish pending snapshot writes, let the recomposer notice them, then release it to
     * do the work.
     *
     * <p>The two drains are not the same drain. The first delivers invalidations and lets any
     * effects that were waiting on the dispatcher run; [BroadcastFrameClock.sendFrame] then resumes
     * the recomposer, whose continuation is itself queued, so it takes a second drain to actually
     * apply the changes within this frame instead of the next one.
     */
    fun onFrame(nanos: Long) {
        if (isDisposed) return
        Snapshot.sendApplyNotifications()
        dispatcher.drain()
        clock.sendFrame(nanos)
        dispatcher.drain()
        // Last, and after recomposition: a node whose style block read state that has just changed
        // re-applies its blocks here, with no composition involved. Still before
        // ModularUI#calculateStyleAndLayout, which is what the frame callback runs ahead of, so the
        // change is laid out in this frame rather than the next.
        nodeObserver.flush()
    }

    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        nodeObserver.dispose()
        composition.dispose()
        recomposer.cancel()
        scope.cancel()
        // Cancelling a suspended coroutine resumes it *through the dispatcher*, and this dispatcher
        // only runs what it is told to. Without this last drain, every `LaunchedEffect` sitting at a
        // suspension point would be cancelled but never resumed, so its `finally` — which is where
        // that effect's cleanup lives — would silently never run.
        dispatcher.drain()
    }
}
