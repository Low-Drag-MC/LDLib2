package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.Snapshot
import com.lowdragmc.lowdraglib2.LDLib2
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext

/**
 * Runs continuations when told to, instead of when they are submitted.
 *
 * <p>This is what keeps the whole Compose machine on the render thread. Recomposition, effects and
 * frame callbacks all funnel through here and only run inside [ComposeHost.onFrame], so nothing
 * Compose does can touch the UI tree from another thread — which matters, because none of LDLib2's
 * UI is thread-safe.
 *
 * <p>Submission, though, can come from anywhere: a `delay()` in an effect resumes from kotlinx's own
 * timer thread, and `withContext(Dispatchers.IO)` comes back from a pool thread. Hence a concurrent
 * queue — the continuation still only *runs* here, on the render thread, at the next drain.
 */
internal class RenderThreadDispatcher : CoroutineDispatcher() {
    private val queue = ConcurrentLinkedQueue<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.add(block)
    }

    /**
     * Runs what is queued, including what gets queued while running — the recomposer resumes itself
     * that way, and making those wait for the next frame would stall the composition by one.
     *
     * <p>Bounded, though. An effect that yields in a loop re-queues itself forever, and an unbounded
     * drain would never hand the render thread back.
     */
    fun drain() {
        var budget = MAX_CONTINUATIONS_PER_DRAIN
        while (budget-- > 0) {
            val block = queue.poll() ?: return
            try {
                block.run()
            } catch (e: Throwable) {
                LDLib2.LOGGER.error("Uncaught error while running a Compose continuation", e)
            }
        }
        LDLib2.LOGGER.warn(
            "A Compose composition queued more than {} continuations in one frame; the rest run next frame. " +
                "An effect is probably yielding in a loop.",
            MAX_CONTINUATIONS_PER_DRAIN,
        )
    }

    companion object {
        private const val MAX_CONTINUATIONS_PER_DRAIN = 10_000
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
        "or launch with `gradlew runClient` instead. Nothing is wrong with the mod itself, " +
        "and this cannot happen outside a development environment."

/**
 * Owns one composition and drives it from LDLib2's render loop.
 *
 * <p>Compose's runtime is deliberately UI-toolkit agnostic — the two things it needs from a host are
 * somewhere to apply tree edits ([UIApplier]) and something to tell it when a frame happens. This
 * supplies both, so the recomposition engine, `remember`, the snapshot system and effects all work
 * exactly as they do on Android, against LDLib2's own element tree.
 *
 * <p>Two-stage on purpose. Constructing one and calling [setContent] builds the element tree
 * synchronously, which is all a UI needs to be laid out. Following state changes needs the recomposer
 * loop and the node observer, and both of those register themselves with the *global* snapshot system
 * — so they only start in [start], once the element is actually on a screen, and stop in [dispose].
 * An element that is built and never shown is then just garbage, instead of something the snapshot
 * system holds on to for the rest of the session.
 *
 * <p>Internal: [ComposeElement] is the public way in, and it is what supplies [LocalUIElement].
 *
 * @param restoredState what an earlier composition's [saveState] returned, so `rememberSaveable`
 *   calls here start from those values. Null starts everything fresh.
 */
internal class ComposeHost(root: UIElement, restoredState: Map<String, List<Any?>>? = null) {
    private val clock = BroadcastFrameClock()
    private val dispatcher = RenderThreadDispatcher()

    /**
     * Tracks the state that each node's `layout` / `style` blocks read, so those blocks re-run when
     * it changes. See [NodeObserver] for why the composition cannot do this by itself.
     */
    private val nodeObserver = NodeObserver()

    /**
     * Accepts every value: saved state never leaves memory, so there is nothing to serialise and no
     * reason to make callers write a `Saver`. See [ComposeSavedState] for what that costs.
     */
    private val saveableRegistry = SaveableStateRegistry(restoredState) { true }
    private val errorHandler = CoroutineExceptionHandler { _, e ->
        LDLib2.LOGGER.error("Compose recomposition failed; this composition is now stopped", e)
    }
    private val scope = CoroutineScope(dispatcher + clock + errorHandler)
    private val recomposer = Recomposer(scope.coroutineContext)
    // Controlled rather than plain only for invalidateAll(), which start() needs; same implementation.
    private val composition = ControlledComposition(UIApplier(root), recomposer)
    private var started = false

    var isDisposed: Boolean = false
        private set

    /**
     * Sets — or replaces — what this composition renders. The initial composition runs synchronously,
     * so the element tree is fully built by the time this returns.
     */
    fun setContent(content: @Composable () -> Unit) {
        check(!isDisposed) { "This ComposeHost has been disposed" }
        composition.setContent {
            CompositionLocalProvider(
                LocalNodeObserver provides nodeObserver,
                LocalSaveableStateRegistry provides saveableRegistry,
            ) { content() }
        }
    }

    /**
     * Starts following state changes. Idempotent; called whenever the element is (re)bound to a UI.
     *
     * <p>Anything that changed between the initial composition and now — an effect writing the first
     * value of a subscription, say — happened while nothing was listening, so the whole composition is
     * invalidated once here. That costs one recomposition per screen opened.
     */
    fun start() {
        if (started || isDisposed) return
        started = true
        nodeObserver.start()
        try {
            scope.launch { recomposer.runRecomposeAndApplyChanges() }
        } catch (e: IllegalAccessError) {
            throw IllegalStateException(COROUTINE_AGENT_ADVICE, e)
        }
        // Let the recomposer reach its first suspension point, so it is already waiting on the clock
        // by the time the first frame arrives.
        dispatcher.drain()
        composition.invalidateAll()
    }

    /**
     * What every `rememberSaveable` in this composition holds right now, in the form [ComposeHost]
     * takes back as `restoredState`. Only meaningful before [dispose], while they are still registered.
     */
    fun saveState(): Map<String, List<Any?>> = saveableRegistry.performSave()

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
        if (isDisposed || !started) return
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

    /**
     * Tears everything down, running every `DisposableEffect`'s cleanup.
     *
     * <p>Exception-safe: user code runs in here (every `onDispose`), and if any of it throws, the
     * global hooks must still come off, or the composition stays reachable from the snapshot system
     * forever. The first failure is rethrown once the teardown is complete.
     */
    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        var failure: Throwable? = null
        try {
            composition.dispose()
        } catch (e: Throwable) {
            failure = e
        }
        nodeObserver.dispose()
        recomposer.cancel()
        scope.cancel()
        // Cancelling a suspended coroutine resumes it *through the dispatcher*, and this dispatcher
        // only runs what it is told to. Without this last drain, every `LaunchedEffect` sitting at a
        // suspension point would be cancelled but never resumed, so its `finally` — which is where
        // that effect's cleanup lives — would silently never run.
        dispatcher.drain()
        failure?.let { throw it }
    }
}
