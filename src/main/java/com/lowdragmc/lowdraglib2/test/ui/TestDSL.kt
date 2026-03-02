package com.lowdragmc.lowdraglib2.test.ui

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.TrackData
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.getValue
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.map
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.setValue
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.*
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D
import com.lowdragmc.lowdraglib2.gui.ui.elements.*
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties
import com.lowdragmc.lowdraglib2.gui.ui.layout.auto
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.Transition
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider
import com.lowdragmc.lowdraglib2.math.interpolate.Eases
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.utils.animation.Animation
import dev.vfyjxf.taffy.style.AlignContent
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.TaffyDirection
import dev.vfyjxf.taffy.style.TaffyDisplay
import dev.vfyjxf.taffy.style.TaffyPosition
import lombok.NoArgsConstructor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import java.util.*
import java.util.function.Consumer


@LDLRegisterClient(name = "dsl", registry = "ldlib2:screen_test")
@NoArgsConstructor
class TestDSL : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI? {
        element({
            layout = {
                position(TaffyPosition.ABSOLUTE)
                minWidth(1)
                alignItems(AlignItems.CENTER)
                alignContent(AlignContent.CENTER)
            }
            style = {
                overflowVisible(false)
                tooltips()
                transition(Transition(mapOf(LayoutProperties.HEIGHT to Animation(1f, 0f, Eases.LINEAR))))
            }
            cls = {
                +"add-class"
                -"remove-class"
            }
            layout = { width(40.pct) }
            style = { background(MCSprites.RECT) }
        }) { }.styleDsl {
            background(MCSprites.RECT)
        }
        return ModularUI.of(UI.of(
            element({
                layout = { size(200.px); gap{ all(3.px) }; padding { all(4.px) } }
                style = {
                    background(Sprites.RECT);
                    opacity(0f);
                    transform2D(Transform2D().translate(0f, 40f))
                }
                cls = { +"cla" }
            }) {
                // float up
                animation {
                    ease(Eases.QUART_IN_OUT)
                    lss("transform", "") // empty as identity
                    lss("opacity", "1") // empty as identity
//                    style(PropertyRegistry.TRANSFORM_2D, Transform2D()) // equal to
//                    style(PropertyRegistry.OPACITY, 1f) // equal to
                }
                element( {
                    layout = { size(30.px) }
                    style = { background(Sprites.RECT_SOLID).tooltips("animation") }
                }) {
                    events { e ->
                        UIEvents.CLICK += {
                            e.animationDsl {
                                duration(1f)
                                ease(Eases.QUAD_IN_OUT)
                                style(PropertyRegistry.TRANSFORM_2D,
                                    Transform2D().scale(0.5f).translate(100f, 0f))
                                style(PropertyRegistry.OPACITY, 0f)
                                onFinished {
                                    e.animationDsl {
                                        ease(Eases.QUART_IN_OUT)
                                        style(PropertyRegistry.TRANSFORM_2D, Transform2D())
                                        style(PropertyRegistry.OPACITY, 1f)
                                    }
                                }
                            }
                        }
                    }
                }
                var value = "hello"
                label {
                    dataSource({ Component.literal(value) })
                }
                button({
                    text("hello <-> world")
                    onClick = {
                        value = if (value == "hello") "world" else "hello"
                    }
                })

                var number = 10.4f
                textField {
                    observer { number = it.toFloatOrNull() ?: number }
                    dataSource { number.toString() }
                }.asNumeric(0.3f, 100f)

                val trackData = TrackData("10.4");
                var trackNumber by trackData.map({ it.toFloatOrNull() ?: 1f }, { it.toString() })
                textField {
                    observer(trackData)
                    dataSource(trackData)
                }.asNumeric(0.3f, 100f)
                button({
                    text("track data + 10")
                    onClick = { trackNumber += 10f }
                })

                row({layout = { gap { all(2.px) } }}) {
                    fluidSlot()
                    itemSlot({ item = Items.APPLE.defaultInstance })
                    column {
                        switch()
                        toggle()
                    }
                    var block = Blocks.DIRT
                    searchComponent({
                        layout = {flex(1)}
                        selectedValue = block
                        candidateUIProvider = UIElementProvider.iconText(
                            { block: Block? -> ItemStackTexture(block!!.asItem()) },
                            { block: Block? -> Component.translatable(block!!.getDescriptionId()) }
                        )
                        searchUI {
                            resultText {
                                BuiltInRegistries.BLOCK.getKey(it).toString()
                            }
                            search { word, handler ->
                                val lowerWord: String = word.lowercase(Locale.getDefault())
                                for (key in BuiltInRegistries.BLOCK.keySet()) {
                                    if (Thread.currentThread().isInterrupted()) break
                                    if (key.toString().lowercase(Locale.getDefault()).contains(lowerWord)) {
                                        handler.acceptResult(BuiltInRegistries.BLOCK.get(key))
                                    }
                                }
                            }
                            onSelected {
                                block = it ?: Blocks.DIRT
                            }
                        }
                    }){}
                }
            }
        ), entityPlayer)
    }
}