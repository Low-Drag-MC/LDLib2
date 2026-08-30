package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.ComposeElement
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.ScrollView
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.compose.rememberServerMessenger
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister
import com.lowdragmc.lowdraglib2.test.ui.IMenuTest
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * A composition whose shape is decided by the server: `/ldlib2_screen_test` cannot open this one —
 * it goes through the real menu path, so a scenario opens it with
 * `PlayerUIMenuType.openUI(player, ldlib2:compose_server)`.
 *
 * <p>The point is the thing that does not work naively. `UISyncManager` numbers sync values and RPC
 * events by registration order, and both sides get their numbering by running this same `createUI`.
 * A composition runs on the client only and adds and removes elements as state changes — so if rows
 * registered anything, the two sides would disagree about what index means what.
 *
 * <p>So nothing in the composition registers. The whole model rides **one** sync value declared
 * below, and the rows send **named messages** back, which share one RPC event however many of them
 * there are. Between those two the row count is free to be anything, and the numbering never moves.
 *
 * <p>Client and server each build their own instance through `PlayerUIMenuType`; [CLIENT] and
 * [SERVER] hand the scenario both, so it can assert on the authoritative state and the displayed
 * state separately.
 */
@LDLRegister(name = "compose_server", registry = "ldlib2:menu_test")
class TestComposeServer : IMenuTest {

    /** The authoritative list. Only ever mutated on the server. */
    private var rows: List<String> = listOf("alpha", "bravo", "charlie")

    /** Names handed out by the server, so an added row is distinguishable. */
    private var nextRow = 1

    /** The client's mirror of [rows], as composition state. Untouched on the server. */
    private val model: MutableState<List<String>> = mutableStateOf(emptyList())

    val serverRows: List<String> get() = rows

    override fun createUI(player: Player): ModularUI {
        val root = UIElement().setId("root")
        root.layout { layout ->
            layout.width(220f)
            layout.height(160f)
            layout.paddingAll(6f)
            layout.gapAll(4f)
        }
        root.style { style -> style.backgroundTexture(Sprites.BORDER) }

        // The composition's host. Built on both sides — only its *content* is client-only — which is
        // what lets the message handlers below sit on the same element the composition sends from.
        val host = ComposeElement { ServerDrivenRows(model) }

        // ---- everything that registers lives here, outside the composition ----------------------
        //
        // One sync value for the entire model. Three rows or three hundred, it is this one slot, so
        // its index is the same on both sides no matter what the UI does with the contents.
        root.addChild(
            BindableValue<String>().bind(
                DataBindingBuilder.stringS2C { rows.joinToString(SEPARATOR) }
                    .name("rows")
                    // Only ever runs on the client: bind() makes the element itself the data source
                    // on the receiving side, so the getter above is the server's half and this is
                    // the client's.
                    .onRemoteSyncReceived { encoded -> model.value = decode(encoded) }
                    .build()
            ).setId("rows_carrier")
        )

        // Messages are addressed by name and share a single RPC event, so the composition can send
        // any of these without registering anything of its own. Registered on both sides here, which
        // is what puts that shared event at the same index on each.
        host.onMessage("add") { rows = rows + "row ${nextRow++}" }
        host.onMessage("remove") { tag -> rows = rows - tag.getString("row") }
        host.onMessage("clear") { rows = emptyList() }

        root.addChild(host)

        if (player.level().isClientSide) CLIENT = this else SERVER = this
        return ModularUI(
            UI.of(root, listOf(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))),
            player,
        )
    }

    companion object {
        /**
         * Joining with a delimiter is the demo being small, not a recommendation: one sync value can
         * carry anything the sync layer can serialise, and a real model would use a CompoundTag or a
         * registered type rather than a string. What matters here is that it is *one* value.
         */
        private const val SEPARATOR = "|"

        @JvmStatic
        @Volatile
        var CLIENT: TestComposeServer? = null

        @JvmStatic
        @Volatile
        var SERVER: TestComposeServer? = null

        private fun decode(encoded: String?): List<String> =
            encoded?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()
    }
}

/** Row names are server-generated and contain spaces; class names cannot. */
private fun slug(row: String) = row.replace(' ', '-')

/**
 * One row per item the server says exists.
 *
 * <p>Nothing here knows how many rows there will be, and nothing here registers anything — the row
 * count is read out of composition state that the sync value writes into, and the buttons talk back
 * by name.
 */
@Composable
private fun ServerDrivenRows(model: MutableState<List<String>>) {
    val rows by model
    val server = rememberServerMessenger()

    Box(layout = { gap { all(3.px) } }, classes = "cs-root") {
        Text(Component.literal("rows: ${rows.size}"), classes = "cs-count")

        Row(layout = { gap { all(3.px) } }) {
            Button(
                onClick = { server.send("add") },
                text = Component.literal("+"),
                layout = { width(30.px) },
                classes = "cs-add",
            )
            Button(
                onClick = { server.send("clear") },
                text = Component.literal("清空"),
                classes = "cs-clear",
            )
        }

        ScrollView(layout = { width(200.px); height(90.px); gap { all(2.px) } }, classes = "cs-list") {
            rows.forEach { row ->
                key(row) {
                    Row(layout = { gap { all(2.px) } }, classes = "cs-row cs-row-${slug(row)}") {
                        Text(Component.literal(row), layout = { width(120.px) })
                        Button(
                            onClick = { server.send("remove") { putString("row", row) } },
                            text = Component.literal("x"),
                            layout = { width(14.px) },
                            classes = "cs-remove cs-remove-${slug(row)}",
                        )
                    }
                }
            }
        }
    }
}
