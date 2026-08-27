package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap
import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.LP
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties
import com.lowdragmc.lowdraglib2.gui.ui.style.Property
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry
import dev.vfyjxf.taffy.style.AlignContent
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import dev.vfyjxf.taffy.style.FlexWrap
import dev.vfyjxf.taffy.style.LengthPercentageAuto
import dev.vfyjxf.taffy.style.TaffyDimension
import dev.vfyjxf.taffy.style.TaffyDisplay
import dev.vfyjxf.taffy.style.TaffyPosition
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.appliedenergistics.yoga.numeric.FloatOptional

// The unit vocabulary is the existing one from the builder DSL — `40.px`, `50.pct`, `auto` mean the
// same thing here as they do in `layout { }`, so there is only one set of units to learn.
private fun LP.toDimension(): TaffyDimension = when (this) {
    is LP.Px -> TaffyDimension.length(value)
    is LP.Percent -> TaffyDimension.percent(value / 100f)
    LP.Auto -> TaffyDimension.auto()
}

private fun LP.toLengthPercentageAuto(): LengthPercentageAuto = when (this) {
    is LP.Px -> LengthPercentageAuto.length(value)
    is LP.Percent -> LengthPercentageAuto.percent(value / 100f)
    LP.Auto -> LengthPercentageAuto.AUTO
}

// region generic escape hatches

/** Sets any layout property directly, for the ones without sugar below. */
fun <T : Any> UIModifier.layout(property: Property<T>, value: T): UIModifier =
    layoutProp(property, value)

/** Sets any visual style property directly, for the ones without sugar below. */
fun <T : Any> UIModifier.style(property: Property<T>, value: T): UIModifier =
    styleProp(property, value)

// endregion

// region size

fun UIModifier.width(value: LP): UIModifier = layoutProp(LayoutProperties.WIDTH, value.toDimension())
fun UIModifier.width(px: Number): UIModifier = width(LP.Px(px.toFloat()))

fun UIModifier.height(value: LP): UIModifier = layoutProp(LayoutProperties.HEIGHT, value.toDimension())
fun UIModifier.height(px: Number): UIModifier = height(LP.Px(px.toFloat()))

fun UIModifier.size(width: LP, height: LP): UIModifier = width(width).height(height)
fun UIModifier.size(value: LP): UIModifier = size(value, value)
fun UIModifier.size(px: Number): UIModifier = size(LP.Px(px.toFloat()))

fun UIModifier.minWidth(value: LP): UIModifier = layoutProp(LayoutProperties.MIN_WIDTH, value.toDimension())
fun UIModifier.minHeight(value: LP): UIModifier = layoutProp(LayoutProperties.MIN_HEIGHT, value.toDimension())
fun UIModifier.maxWidth(value: LP): UIModifier = layoutProp(LayoutProperties.MAX_WIDTH, value.toDimension())
fun UIModifier.maxHeight(value: LP): UIModifier = layoutProp(LayoutProperties.MAX_HEIGHT, value.toDimension())

fun UIModifier.aspectRatio(ratio: Float): UIModifier =
    layoutProp(LayoutProperties.ASPECT_RATE, FloatOptional.of(ratio))

// endregion

// region spacing

fun UIModifier.padding(value: LP): UIModifier =
    layoutProp(LayoutProperties.PADDING_ALL, value.toLengthPercentageAuto())

fun UIModifier.padding(px: Number): UIModifier = padding(LP.Px(px.toFloat()))

fun UIModifier.paddingHorizontal(value: LP): UIModifier =
    layoutProp(LayoutProperties.PADDING_HORIZONTAL, value.toLengthPercentageAuto())

fun UIModifier.paddingVertical(value: LP): UIModifier =
    layoutProp(LayoutProperties.PADDING_VERTICAL, value.toLengthPercentageAuto())

fun UIModifier.margin(value: LP): UIModifier =
    layoutProp(LayoutProperties.MARGIN_ALL, value.toLengthPercentageAuto())

fun UIModifier.margin(px: Number): UIModifier = margin(LP.Px(px.toFloat()))

fun UIModifier.marginHorizontal(value: LP): UIModifier =
    layoutProp(LayoutProperties.MARGIN_HORIZONTAL, value.toLengthPercentageAuto())

fun UIModifier.marginVertical(value: LP): UIModifier =
    layoutProp(LayoutProperties.MARGIN_VERTICAL, value.toLengthPercentageAuto())

fun UIModifier.gap(value: LP): UIModifier =
    layoutProp(LayoutProperties.GAP_ALL, value.toLengthPercentageAuto())

fun UIModifier.gap(px: Number): UIModifier = gap(LP.Px(px.toFloat()))

// endregion

// region flex & placement

fun UIModifier.flex(value: Float): UIModifier = layoutProp(LayoutProperties.FLEX, FloatOptional.of(value))
fun UIModifier.flexGrow(value: Float): UIModifier = layoutProp(LayoutProperties.FLEX_GROW, FloatOptional.of(value))
fun UIModifier.flexShrink(value: Float): UIModifier = layoutProp(LayoutProperties.FLEX_SHRINK, FloatOptional.of(value))
fun UIModifier.flexDirection(value: FlexDirection): UIModifier = layoutProp(LayoutProperties.FLEX_DIRECTION, value)
fun UIModifier.flexWrap(value: FlexWrap): UIModifier = layoutProp(LayoutProperties.FLEX_WRAP, value)
fun UIModifier.display(value: TaffyDisplay): UIModifier = layoutProp(LayoutProperties.DISPLAY, value)

fun UIModifier.alignItems(value: AlignItems): UIModifier = layoutProp(LayoutProperties.ALIGN_ITEMS, value)
fun UIModifier.alignSelf(value: AlignItems): UIModifier = layoutProp(LayoutProperties.ALIGN_SELF, value)
fun UIModifier.alignContent(value: AlignContent): UIModifier = layoutProp(LayoutProperties.ALIGN_CONTENT, value)
fun UIModifier.justifyContent(value: AlignContent): UIModifier = layoutProp(LayoutProperties.JUSTIFY_CONTENT, value)

fun UIModifier.position(value: TaffyPosition): UIModifier = layoutProp(LayoutProperties.POSITION, value)
fun UIModifier.left(value: LP): UIModifier = layoutProp(LayoutProperties.LEFT, value.toLengthPercentageAuto())
fun UIModifier.top(value: LP): UIModifier = layoutProp(LayoutProperties.TOP, value.toLengthPercentageAuto())
fun UIModifier.right(value: LP): UIModifier = layoutProp(LayoutProperties.RIGHT, value.toLengthPercentageAuto())
fun UIModifier.bottom(value: LP): UIModifier = layoutProp(LayoutProperties.BOTTOM, value.toLengthPercentageAuto())

// endregion

// region appearance

fun UIModifier.background(texture: IGuiTexture): UIModifier = styleProp(PropertyRegistry.BACKGROUND, texture)
fun UIModifier.overlay(texture: IGuiTexture): UIModifier = styleProp(PropertyRegistry.OVERLAY, texture)
fun UIModifier.opacity(value: Float): UIModifier = styleProp(PropertyRegistry.OPACITY, value)
fun UIModifier.zIndex(value: Int): UIModifier = styleProp(PropertyRegistry.Z_INDEX, value)
fun UIModifier.color(argb: Int): UIModifier = styleProp(PropertyRegistry.COLOR, argb)
fun UIModifier.transform2D(transform: Transform2D): UIModifier = styleProp(PropertyRegistry.TRANSFORM_2D, transform)
fun UIModifier.tooltips(tooltips: Tooltips): UIModifier = styleProp(PropertyRegistry.TOOLTIPS, tooltips)
fun UIModifier.tooltips(vararg lines: Component): UIModifier = tooltips(Tooltips.of(*lines))
fun UIModifier.tooltips(vararg lines: String): UIModifier = tooltips(Tooltips.of(*lines))

/** Adds stylesheet classes, so `.name { }` rules match this element. */
fun UIModifier.classes(vararg names: String): UIModifier =
    names.fold(this) { acc, name -> acc.then(ClassOp(name)) }

fun UIModifier.id(id: String): UIModifier = then(IdOp(id))

/**
 * Greys the element out and stops it responding to input.
 *
 * ```kotlin
 * Button(onClick = { submit() }, modifier = UIModifier.enabled(form.isValid), text = ok)
 * ```
 */
fun UIModifier.enabled(enabled: Boolean): UIModifier = then(ActiveOp(enabled))

/**
 * Hides the element without removing it.
 *
 * <p>It keeps its place in the layout — for "gone entirely", leave it out of the composition with an
 * `if` instead, which also lets its effects clean up.
 */
fun UIModifier.visible(visible: Boolean): UIModifier = then(VisibleOp(visible))

/** Lets the element take keyboard focus. Off by default for everything but input widgets. */
fun UIModifier.focusable(focusable: Boolean = true): UIModifier = then(FocusableOp(focusable))

// endregion

// region text
//
// These apply to any element with a text style — Text, Label, Button captions, TextField. They are
// ordinary style properties: every Style facade on an element shares one StyleBag, so a text
// property set here lands in exactly the same place `textStyle { }` would put it.

fun UIModifier.fontSize(size: Float): UIModifier = styleProp(PropertyRegistry.FONT_SIZE, size)
fun UIModifier.font(font: ResourceLocation): UIModifier = styleProp(PropertyRegistry.FONT, font)
fun UIModifier.textColor(argb: Int): UIModifier = styleProp(PropertyRegistry.TEXT_COLOR, argb)
fun UIModifier.textShadow(shadow: Boolean): UIModifier = styleProp(PropertyRegistry.TEXT_SHADOW, shadow)
fun UIModifier.lineSpacing(spacing: Float): UIModifier = styleProp(PropertyRegistry.LINE_SPACING, spacing)
fun UIModifier.textWrap(wrap: TextWrap): UIModifier = styleProp(PropertyRegistry.TEXT_WRAP, wrap)
fun UIModifier.rollSpeed(speed: Float): UIModifier = styleProp(PropertyRegistry.ROLL_SPEED, speed)

fun UIModifier.textAlign(horizontal: Horizontal): UIModifier =
    styleProp(PropertyRegistry.HORIZONTAL_ALIGN, horizontal)

fun UIModifier.textAlign(vertical: Vertical): UIModifier =
    styleProp(PropertyRegistry.VERTICAL_ALIGN, vertical)

fun UIModifier.textAlign(horizontal: Horizontal, vertical: Vertical): UIModifier =
    textAlign(horizontal).textAlign(vertical)

/**
 * Lets the element size itself to its text.
 *
 * <p>Rarely needed directly: [com.lowdragmc.lowdraglib2.gui.ui.compose.Text] turns adaptive height
 * on by itself unless the chain supplies a height. Reach for this to make the *width* follow the
 * text as well, or to force the behaviour on a widget that is not a plain `Text`.
 *
 * <p>Note that adaptive sizing writes the measured size at `IMPORTANT` origin, which outranks
 * everything a modifier can set — so an adaptive axis wins over [width]/[height] on that same axis.
 */
fun UIModifier.adaptiveWidth(adaptive: Boolean = true): UIModifier =
    styleProp(PropertyRegistry.ADAPTIVE_WIDTH, adaptive)

fun UIModifier.adaptiveHeight(adaptive: Boolean = true): UIModifier =
    styleProp(PropertyRegistry.ADAPTIVE_HEIGHT, adaptive)

// endregion

// region events

/**
 * Registers a handler for any event type.
 *
 * <p>Composable because the handler is kept behind a [EventHandlerHolder] that has to survive
 * recomposition — see that class for why the obvious alternative churns listeners every frame.
 */
@Composable
fun UIModifier.onEvent(
    type: String,
    capture: Boolean = false,
    handler: (UIEvent) -> Unit,
): UIModifier {
    val holder = remember { EventHandlerHolder() }
    holder.handler = handler
    return then(EventOp(type, capture, holder))
}

/**
 * Fires on [UIEvents.CLICK] — press and release on the same element.
 *
 * <p>Note that [com.lowdragmc.lowdraglib2.gui.ui.elements.Button]'s own `onClick` is a different
 * thing: it fires on mouse-*down* and plays the click sound. `Button(onClick = ...)` keeps that
 * behaviour; this modifier is the general one, for elements that are not buttons.
 */
@Composable
fun UIModifier.onClick(handler: (UIEvent) -> Unit): UIModifier = onEvent(UIEvents.CLICK, false, handler)

@Composable
fun UIModifier.onMouseDown(handler: (UIEvent) -> Unit): UIModifier = onEvent(UIEvents.MOUSE_DOWN, false, handler)

@Composable
fun UIModifier.onMouseUp(handler: (UIEvent) -> Unit): UIModifier = onEvent(UIEvents.MOUSE_UP, false, handler)

@Composable
fun UIModifier.onMouseEnter(handler: (UIEvent) -> Unit): UIModifier = onEvent(UIEvents.MOUSE_ENTER, false, handler)

@Composable
fun UIModifier.onMouseLeave(handler: (UIEvent) -> Unit): UIModifier = onEvent(UIEvents.MOUSE_LEAVE, false, handler)

@Composable
fun UIModifier.onMouseWheel(handler: (UIEvent) -> Unit): UIModifier = onEvent(UIEvents.MOUSE_WHEEL, false, handler)

@Composable
fun UIModifier.onKeyDown(handler: (UIEvent) -> Unit): UIModifier = onEvent(UIEvents.KEY_DOWN, false, handler)

@Composable
fun UIModifier.onCharTyped(handler: (UIEvent) -> Unit): UIModifier = onEvent(UIEvents.CHAR_TYPED, false, handler)

// endregion
