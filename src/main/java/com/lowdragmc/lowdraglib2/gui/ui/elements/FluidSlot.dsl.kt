package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import java.util.function.Consumer

/**
 * Extension function for FluidSlot.SlotStyle DSL
 */
fun <T : FluidSlot> T.slotStyleDsl(init: FluidSlot.SlotStyle.() -> Unit = {}): T {
    this.slotStyle.apply(init)
    return this
}

/**
 * Specification for FluidSlot element
 */
open class FluidSlotSpec<T : FluidSlot>(
    var slotStyle: (FluidSlot.SlotStyle.() -> Unit)? = null,
    var fluid: FluidStack? = null,
    var capacity: Int? = null,
    var allowClickFilled: Boolean? = null,
    var allowClickDrained: Boolean? = null,
    var fluidHandler: IFluidHandler? = null,
    var tankIndex: Int? = null,
) : ElementSpec<T>() {
    /**
     * Bind to fluid handler
     */
    fun bind(handler: IFluidHandler, index: Int) = apply {
        this.fluidHandler = handler
        this.tankIndex = index
    }
}

/**
 * FluidSlot element builder
 */
open class FluidSlotElement<T : FluidSlot>(
    element: T,
    spec: (FluidSlotSpec<T>.() -> Unit)? = null,
) : UIContainer<T, FluidSlotSpec<T>>(element, spec) {
    override fun makeSpec(): FluidSlotSpec<T>? {
        return spec?.let { FluidSlotSpec<T>().apply(it) }
    }

    override fun build(spec: FluidSlotSpec<T>?): T {
        val e = super.build(spec)
        applyFluidSlotProperties(spec, e)
        return e
    }

    protected fun applyFluidSlotProperties(spec: FluidSlotSpec<T>?, element: FluidSlot) {
        spec?.slotStyle?.let(element.slotStyle::apply)
        spec?.capacity?.let { element.setCapacity(it) }
        spec?.allowClickFilled?.let { element.setAllowClickFilled(it) }
        spec?.allowClickDrained?.let { element.setAllowClickDrained(it) }

        // Bind to handler if specified
        if (spec?.fluidHandler != null && spec.tankIndex != null) {
            element.bind(spec.fluidHandler!!, spec.tankIndex!!)
        }

        // Set fluid last (after capacity is set)
        spec?.fluid?.let { element.setFluid(it, false) }
    }
}

/**
 * Top Level - Create a standalone FluidSlot element
 */
fun fluidSlot(spec: (FluidSlotSpec<FluidSlot>.() -> Unit)? = null,
              init: FluidSlotElement<FluidSlot>.() -> Unit = {}): FluidSlot {
    return FluidSlotElement(FluidSlot(), spec).apply(init).build()
}

/**
 * Internal Builder - Add FluidSlot as a child to a container
 */
fun UIContainer<*, *>.fluidSlot(spec: (FluidSlotSpec<FluidSlot>.() -> Unit)? = null,
                                 init: FluidSlotElement<FluidSlot>.() -> Unit = {}) =
    add(FluidSlotElement(FluidSlot(), spec), init)

/**
 * DSL converter - Convert existing FluidSlot to DSL builder
 */
fun <T : FluidSlot> T.dsl(spec: (FluidSlotSpec<T>.() -> Unit)? = null,
                          init: FluidSlotElement<T>.() -> Unit = {}): FluidSlotElement<T> {
    return FluidSlotElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set fluid
 */
fun <T : FluidSlot> FluidSlotElement<T>.withFluid(fluid: FluidStack): FluidSlotElement<T> = apply {
    element.setFluid(fluid)
}

/**
 * Extension: Set capacity
 */
fun <T : FluidSlot> FluidSlotElement<T>.withCapacity(capacity: Int): FluidSlotElement<T> = apply {
    element.setCapacity(capacity)
}

/**
 * Extension: Bind to fluid handler
 */
fun <T : FluidSlot> FluidSlotElement<T>.bindTo(handler: IFluidHandler, index: Int): FluidSlotElement<T> = apply {
    element.bind(handler, index)
}

/**
 * Extension: Set fill direction
 */
fun <T : FluidSlot> FluidSlotElement<T>.fillDirection(direction: FillDirection): FluidSlotElement<T> = apply {
    element.slotStyle.fillDirection(direction)
}

/**
 * Extension: Show fluid tooltips
 */
fun <T : FluidSlot> FluidSlotElement<T>.withTooltips(): FluidSlotElement<T> = apply {
    element.slotStyle.showFluidTooltips(true)
}

/**
 * Extension: Allow click to fill containers
 */
fun <T : FluidSlot> FluidSlotElement<T>.allowClickFilled(allow: Boolean = true): FluidSlotElement<T> = apply {
    element.setAllowClickFilled(allow)
}

/**
 * Extension: Allow click to drain containers
 */
fun <T : FluidSlot> FluidSlotElement<T>.allowClickDrained(allow: Boolean = true): FluidSlotElement<T> = apply {
    element.setAllowClickDrained(allow)
}

/**
 * Extension: Configure label
 */
fun <T : FluidSlot> FluidSlotElement<T>.withLabel(config: Label.() -> Unit): FluidSlotElement<T> = apply {
    element.amountLabel.apply(config)
}

// ===========================
// XEI Integration Methods
// ===========================

/**
 * Extension: Mark as JEI/REI/EMI phantom slot
 */
fun <T : FluidSlot> FluidSlotElement<T>.asXeiPhantom(): FluidSlotElement<T> = apply {
    element.xeiPhantom()
}

/**
 * Extension: Mark as recipe ingredient for JEI/REI/EMI
 */
fun <T : FluidSlot> FluidSlotElement<T>.asXeiRecipeIngredient(io: IngredientIO): FluidSlotElement<T> = apply {
    element.xeiRecipeIngredient(io)
}

/**
 * Extension: Mark as recipe slot for JEI/REI/EMI
 */
fun <T : FluidSlot> FluidSlotElement<T>.asXeiRecipeSlot(io: IngredientIO = IngredientIO.NONE, chance: Float = 1f): FluidSlotElement<T> = apply {
    element.xeiRecipeSlot(io, chance)
}
