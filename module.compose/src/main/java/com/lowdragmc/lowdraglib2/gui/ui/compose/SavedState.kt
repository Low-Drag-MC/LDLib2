package com.lowdragmc.lowdraglib2.gui.ui.compose

/**
 * Where [androidx.compose.runtime.saveable.rememberSaveable] state waits while its screen is closed.
 *
 * <p>A [ComposeElement] given a `stateKey` saves into here when it is torn down and restores from
 * here when it is built again with the same key. Without a key nothing is kept across a close;
 * `rememberSaveable` still survives lazy rows scrolling out and tabs being removed, within one
 * composition.
 *
 * <p>Values are kept in memory as they are, not serialised, so no `Saver` is needed — but whatever is
 * saved stays reachable until cleared: save values, not UI elements or things that hold the level.
 *
 * <p>Everything is forgotten when the player leaves a world. Two screens open at once under the same
 * key both restore the same state, and the one closed last is what is kept.
 *
 * <p>Render thread only.
 */
object ComposeSavedState {
    private val saved = HashMap<String, Map<String, List<Any?>>>()

    internal fun restore(key: String): Map<String, List<Any?>>? = saved[key]

    internal fun save(key: String, values: Map<String, List<Any?>>) {
        if (values.isEmpty()) saved.remove(key) else saved[key] = values
    }

    /** Forgets what was saved under [key], so the next screen opened with it starts fresh. */
    @JvmStatic
    fun clear(key: String) {
        saved.remove(key)
    }

    /** Forgets everything. Called automatically when the player leaves a world. */
    @JvmStatic
    fun clearAll() {
        saved.clear()
    }
}
