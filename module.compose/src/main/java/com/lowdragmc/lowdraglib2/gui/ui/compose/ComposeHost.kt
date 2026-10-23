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
 * Runs continuations only when [ComposeHost] drains it (each frame, and once more on dispose), so
 * Compose never touches LDLib2's non-thread-safe UI tree off the render thread. Submission comes from
 * any thread — `delay()` resumes on kotlinx's timer thread — hence the concurrent queue.
 */
internal class RenderThreadDispatcher : CoroutineDispatcher() {
    private val queue = ConcurrentLinkedQueue<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.add(block)
    }

    /**
     * Runs what is queued, including what gets queued meanwhile (the recomposer resumes itself that
     * way), up to a budget so an effect yielding in a loop cannot keep the render thread.
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
 * Explains an `IllegalAccessError` from the first coroutine: IntelliJ's coroutines debug agent rewrites
 * stdlib's `DebugProbesKt` to call into `kotlinx.coroutines.core`, which `kotlin.stdlib` does not read.
 * Not fixable from here (`--add-reads` does not reach ModLauncher's module layer); never in production.
 */
private const val COROUTINE_AGENT_ADVICE =
    "Compose could not start its recomposer coroutine. This is almost certainly the Kotlin " +
        "coroutines debug agent: IntelliJ attaches it automatically, it rewrites kotlin-stdlib's " +
        "DebugProbesKt to call into kotlinx-coroutines, and NeoForge's module layer forbids that. " +
        "Turn the coroutine agent off for this run (IDE settings, search for \"coroutine agent\"), " +
        "or launch with `gradlew runClient` instead. Nothing is wrong with the mod itself, " +
        "and this cannot happen outside a development environment."

/**
 * Owns one composition and drives it from LDLib2's render loop; [ComposeElement] is the public way in.
 *
 * <p>Two-stage: [setContent] builds the element tree synchronously, but the recomposer loop and the
 * node observer register with the *global* snapshot system, so they only start in [start], once the
 * element is on a screen, and stop in [dispose]. An element built and never shown is then just garbage.
 *
 * @param restoredState what an earlier composition's [saveState] returned, so `rememberSaveable`
 *   calls here start from those values. Null starts everything fresh.
 */
internal class ComposeHost(root: UIElement, restoredState: Map<String, List<Any?>>? = null) {
    private val clock = BroadcastFrameClock()
    private val dispatcher = RenderThreadDispatcher()

    /** Re-runs each node's `layout` / `style` blocks when the state they read changes. */
    private val nodeObserver = NodeObserver()

    /** Accepts every value: saved state never leaves memory, so there is nothing to serialise. */
    private val saveableRegistry = SaveableStateRegistry(restoredState) { true }
    private val errorHandler = CoroutineExceptionHandler { _, e ->
        LDLib2.LOGGER.error("Compose recomposition failed; this composition is now stopped", e)
    }
    private val scope = CoroutineScope(dispatcher + clock + errorHandler)
    private val recomposer = Recomposer(scope.coroutineContext)
    // Controlled only for invalidateAll(), which start() needs.
    private val composition = ControlledComposition(UIApplier(root), recomposer)
    private var started = false

    var isDisposed: Boolean = false
        private set

    /** Sets or replaces the content. The initial composition runs synchronously. */
    fun setContent(content: @Composable () -> Unit) = running {
        check(!isDisposed) { "This ComposeHost has been disposed" }
        composition.setContent {
            CompositionLocalProvider(
                LocalNodeObserver provides nodeObserver,
                LocalSaveableStateRegistry provides saveableRegistry,
            ) { content() }
        }
    }

    /**
     * Starts following state changes; idempotent. Invalidates the whole composition once, since changes
     * made before this (an effect's first write, say) happened while nothing was listening.
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
        // Not drained here: during attach the children are not in the UI yet, and the first effects
        // would see them detached. The first frame's drain starts it.
        composition.invalidateAll()
    }

    /**
     * What every `rememberSaveable` in this composition holds right now, in the form [ComposeHost]
     * takes back as `restoredState`. Only meaningful before [dispose], while they are still registered.
     */
    fun saveState(): Map<String, List<Any?>> = saveableRegistry.performSave()

    /**
     * One frame: publish pending snapshot writes, drain, send the frame (recomposition and applying
     * happen inside [BroadcastFrameClock.sendFrame]), then drain what the frame resumed.
     */
    fun onFrame(nanos: Long) = running {
        if (isDisposed || !started) return@running
        Snapshot.sendApplyNotifications()
        dispatcher.drain()
        clock.sendFrame(nanos)
        dispatcher.drain()
        // After recomposition, and still ahead of ModularUI#calculateStyleAndLayout, so re-run blocks
        // are laid out in this frame rather than the next.
        nodeObserver.flush()
    }

    /**
     * Tears everything down, running every `DisposableEffect`'s cleanup. If an `onDispose` throws, the
     * global snapshot hooks still come off and the first failure is rethrown at the end.
     */
    fun dispose() = running {
        if (isDisposed) return@running
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
        // Cancellation resumes suspended coroutines through the dispatcher; without this drain no
        // suspended `LaunchedEffect`'s `finally` would ever run.
        dispatcher.drain()
        failure?.let { throw it }
    }

    companion object {
        private val depth = ThreadLocal.withInitial { 0 }

        /** Whether a composition is being built or run on this thread, effects included. */
        internal val isRunning: Boolean get() = depth.get() > 0

        private inline fun <R> running(block: () -> R): R {
            depth.set(depth.get() + 1)
            try {
                return block()
            } finally {
                depth.set(depth.get() - 1)
            }
        }
    }
}
