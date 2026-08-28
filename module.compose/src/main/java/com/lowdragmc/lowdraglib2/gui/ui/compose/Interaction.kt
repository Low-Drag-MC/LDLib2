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
 * <p>**For appearance alone you do not need this.** `ModularUI.refreshHoveredElement` already keeps a
 * `__hovered__` class on the hovered element and every ancestor, so a stylesheet rule reaches hover
 * on any element without a line of Kotlin, and costs nothing:
 *
 * ```lss
 * .card.__hovered__ { background: …; }
 * ```
 *
 * <p>What a stylesheet cannot do is change *what is composed*. This is for the cases where hovering
 * has to alter the tree rather than the paint — different text, an extra child, an animation target:
 *
 * ```kotlin
 * val interaction = rememberInteraction()
 * Box(events = { track(interaction) }) {
 *     Text(Component.literal(if (interaction.hovered) "松开以删除" else "删除"))
 * }
 * ```
 *
 * <p>[pressed] has no class equivalent at all — only widgets that implement `base`/`hover`/`pressed`
 * textures themselves know about it — so that half is new either way.
 *
 * <p>Reading either flag in the composable body recomposes on change. Reading one inside a `style`
 * block would not — see "读状态要在函数体里" in compose.md.
 */
@Stable
class InteractionState {
    var hovered by mutableStateOf(false)
        internal set

    /**
     * True between mouse-down on this element and the release, or the pointer leaving it.
     *
     * <p>Cleared on leave as well as on release because a release outside the element never reaches
     * it, and a control stuck in its pressed appearance is worse than one that lets go early.
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
    onMouseLeave {
        state.hovered = false
        state.pressed = false
    }
    onMouseDown { state.pressed = true }
    onMouseUp { state.pressed = false }
}
