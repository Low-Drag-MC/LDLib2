package com.lowdragmc.lowdraglib2.test.ui

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot
import com.lowdragmc.lowdraglib2.gui.sync.rpc.rpcEvent
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.*
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.inventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister
import lombok.NoArgsConstructor
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.templates.FluidTank
import net.neoforged.neoforge.items.ItemStackHandler


@LDLRegister(name = "dsl_sync", registry = "ldlib2:menu_test")
@NoArgsConstructor
class TestMenuDSL : IMenuTest {
    private var bool = true
    private var string = "hello"
    private var number = 0.5f

    override fun createUI(player: Player): ModularUI {
        val itemHandler = ItemStackHandler(2)
        val fluidTank = FluidTank(2000)
        // create a root element
        val root = element({
            cls = { +"panel_bg" }
        }) {
            // add a label to display text
            label({text("Data Between Screen and Menu")})
            row ({
                layout = { gap { all(2.px) } }
            }) {
                itemSlot({bind(itemHandler, 0)})
                itemSlot({bind(ItemHandlerSlot(itemHandler, 1).setCanTake({false}))})
                fluidSlot({bind(fluidTank, 0)})
            }
            // bind value to the components
            element ({
                layout = { gap { all(2.px) } }
            }) {
                switch { bind(::bool) }
                textField { bind(::string) }
                scrollerHorizontal({layout = {width(100.pct)}}) { bind(::number) }
                // read-only (s->c), always get data from the server and display on the client
                label { bindS2C({ Component.literal("s->c only: ")
                        .append(Component.literal(bool.toString()).withStyle(ChatFormatting.AQUA)).append(" ")
                        .append(Component.literal(string).withStyle(ChatFormatting.RED)).append(" ")
                        .append(Component.literal("%.2f".format(number)).withStyle(ChatFormatting.YELLOW))}) }
                // trigger ui events on the server side
                button {
                    serverEvents {
                        UIEvents.MOUSE_DOWN += {
                            if (fluidTank.getFluid().fluid === Fluids.WATER) {
                                fluidTank.setFluid(FluidStack(Fluids.LAVA, 1000))
                            } else {
                                fluidTank.setFluid(FluidStack(Fluids.WATER, 1000))
                            }
                        }
                    }
                    // define a rpc event
                    val rpcEvent = element.rpcEvent { clickValue: String -> string = clickValue }
                    events {
                        UIEvents.MOUSE_DOWN += {
                            rpcEvent.send( "rpc")
                        }
                    }
                }
                // you could also use button.setOnServerClick(e -> { ... })
                inventorySlots()
            }
        }
        return ModularUI(UI.of(root, StylesheetManager.MODERN), player)
    }

}