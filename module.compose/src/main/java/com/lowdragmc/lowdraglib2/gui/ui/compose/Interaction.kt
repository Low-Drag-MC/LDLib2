package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Whether the pointer is over an element, and whether it is being held down, as composition state.
 *
 * <p>**For appearance alone you do not need this:** the hovered element and every ancestor already
 * carry a `__hovered__` class, so a stylesheet rule can style hover:
 *
 * ```lss
 * .card.__hovered__ { background: …; }
 * ```
 *
 * <p>Use this when hovering has to change *what is composed* — different text, an extra child, an
 * animation target:
 *
 * ```kotlin
 * val interaction = rememberInteraction()
 * Box(events = { track(interaction) }) {
 *     Text(Component.literal(if (interaction.hovered) "松开以删除" else "删除"))
 * }
 * ```
 *
 * <p>[pressed] has no class equivalent.
 *
 * <p>Reading either flag in the composable body recomposes on change; reading one inside a `style`
 * or `layout` block re-runs just that block, without recomposing anything.
 */
@Stable
class InteractionState {
    var hovered by mutableStateOf(false)
        internal set

    /**
     * True between mouse-down on this element and the release, or the pointer leaving it — a release
     * outside the element never reaches it.
     */
    var pressed by mutableStateOf(false)
        internal set
}

@Composable
fun rememberInteraction(): InteractionState = remember { InteractionState() }

/**
 * Keeps [state] in step with the pointer.
 *
 * <p>Safe to combine with handlers of your own for the same events, in either order: [EventScope.on]
 * appends rather than replaces.
 */
fun EventScope.track(state: InteractionState) {
    // Enter and leave are capture-only and target the innermost element; see EventScope.onMouseEnter.
    onMouseEnter { state.hovered = true }
    onMouseLeave { event ->
        // Leave also fires for this element's own descendants, while the pointer is still inside it.
        val self = event.currentElement
        if (self != null && self.isAncestorOf(self.modularUI?.lastHoveredElement)) return@onMouseLeave
        state.hovered = false
        state.pressed = false
    }
    onMouseDown { state.pressed = true }
    onMouseUp { state.pressed = false }
}
