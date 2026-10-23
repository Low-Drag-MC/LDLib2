package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player

/**
 * Builds a [UI] whose whole tree is declarative.
 *
 * ```kotlin
 * override fun createUI(player: Player?) =
 *     composeModularUI(player, StylesheetManager.MODERN) { MyScreen() }
 * ```
 *
 * @param stateKey see [ComposeElement.stateKey]: given one, `rememberSaveable` state survives the
 *   screen being closed and opened again.
 */
fun composeUI(
    vararg stylesheets: ResourceLocation,
    stateKey: String? = null,
    content: @Composable () -> Unit,
): UI = UI.of(ComposeElement(stateKey, content), *stylesheets)

/** [composeUI], for stylesheets that are already parsed. */
fun composeUI(
    stylesheets: List<Stylesheet>,
    stateKey: String? = null,
    content: @Composable () -> Unit,
): UI = UI.of(ComposeElement(stateKey, content), stylesheets)

/** [composeUI], wrapped straight into a [ModularUI]. */
fun composeModularUI(
    player: Player? = null,
    vararg stylesheets: ResourceLocation,
    stateKey: String? = null,
    content: @Composable () -> Unit,
): ModularUI = ModularUI.of(composeUI(*stylesheets, stateKey = stateKey, content = content), player)
