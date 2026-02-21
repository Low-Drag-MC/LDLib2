package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.Element
import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandlerModifiable

/**
 * Extension function for ItemSlot.SlotStyle DSL
 */
fun <T : ItemSlot> T.slotStyleDsl(init: ItemSlot.SlotStyle.() -> Unit = {}): T {
    this.slotStyle.apply(init)
    return this
}

/**
 * Specification for ItemSlot element
 */
open class ItemSlotSpec<T : ItemSlot>(
    var slotStyle: (ItemSlot.SlotStyle.() -> Unit)? = null,
    var item: ItemStack? = null,
    var itemHandler: IItemHandlerModifiable? = null,
    var slotIndex: Int? = null,
    var slot: Slot? = null,
) : ElementSpec<T>() {
    /**
     * Bind to item handler
     */
    fun bind(handler: IItemHandlerModifiable, index: Int) = apply {
        this.itemHandler = handler
        this.slotIndex = index
    }

    /**
     * Bind to slot
     */
    fun bind(slot: Slot) = apply {
        this.slot = slot
    }
}

/**
 * ItemSlot element builder
 */
open class ItemSlotElement<T : ItemSlot>(
    element: T,
    spec: (ItemSlotSpec<T>.() -> Unit)? = null,
) : UIContainer<T, ItemSlotSpec<T>>(element, spec) {
    override fun makeSpec(): ItemSlotSpec<T>? {
        return spec?.let { ItemSlotSpec<T>().apply(it) }
    }

    override fun build(spec: ItemSlotSpec<T>?): T {
        val e = super.build(spec)
        applyItemSlotProperties(spec, e)
        return e
    }

    protected fun applyItemSlotProperties(spec: ItemSlotSpec<T>?, element: ItemSlot) {
        spec?.slotStyle?.let(element.slotStyle::apply)

        // Bind to handler or slot if specified
        if (spec?.itemHandler != null && spec.slotIndex != null) {
            element.bind(spec.itemHandler!!, spec.slotIndex!!)
        } else if (spec?.slot != null) {
            element.bind(spec.slot!!)
        }

        // Set item last
        spec?.item?.let { element.setItem(it, false) }
    }
}

/**
 * Top Level - Create a standalone ItemSlot element
 */
fun itemSlot(spec: (ItemSlotSpec<ItemSlot>.() -> Unit)? = null,
             init: ItemSlotElement<ItemSlot>.() -> Unit = {}): ItemSlot {
    return ItemSlotElement(ItemSlot(), spec).apply(init).build()
}

/**
 * Top Level - Create ItemSlot with specific slot
 */
fun itemSlot(slot: Slot,
             spec: (ItemSlotSpec<ItemSlot>.() -> Unit)? = null,
             init: ItemSlotElement<ItemSlot>.() -> Unit = {}): ItemSlot {
    return ItemSlotElement(ItemSlot(slot), spec).apply(init).build()
}

/**
 * Internal Builder - Add ItemSlot as a child to a container
 */
fun UIContainer<*, *>.itemSlot(spec: (ItemSlotSpec<ItemSlot>.() -> Unit)? = null,
                                init: ItemSlotElement<ItemSlot>.() -> Unit = {}) =
    add(ItemSlotElement(ItemSlot(), spec), init)

/**
 * Internal Builder - Add ItemSlot with specific slot as a child to a container
 */
fun UIContainer<*, *>.itemSlot(slot: Slot,
                                spec: (ItemSlotSpec<ItemSlot>.() -> Unit)? = null,
                                init: ItemSlotElement<ItemSlot>.() -> Unit = {}) =
    add(ItemSlotElement(ItemSlot(slot), spec), init)

/**
 * DSL converter - Convert existing ItemSlot to DSL builder
 */
fun <T : ItemSlot> T.dsl(spec: (ItemSlotSpec<T>.() -> Unit)? = null,
                         init: ItemSlotElement<T>.() -> Unit = {}): ItemSlotElement<T> {
    return ItemSlotElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set item
 */
fun <T : ItemSlot> ItemSlotElement<T>.withItem(item: ItemStack): ItemSlotElement<T> = apply {
    element.setItem(item)
}

/**
 * Extension: Bind to item handler
 */
fun <T : ItemSlot> ItemSlotElement<T>.bindTo(handler: IItemHandlerModifiable, index: Int): ItemSlotElement<T> = apply {
    element.bind(handler, index)
}

/**
 * Extension: Bind to slot
 */
fun <T : ItemSlot> ItemSlotElement<T>.bindTo(slot: Slot): ItemSlotElement<T> = apply {
    element.bind(slot)
}

/**
 * Extension: Show item tooltips
 */
fun <T : ItemSlot> ItemSlotElement<T>.withTooltips(): ItemSlotElement<T> = apply {
    element.slotStyle.showItemTooltips(true)
}

/**
 * Extension: Mark as player slot
 */
fun <T : ItemSlot> ItemSlotElement<T>.asPlayerSlot(): ItemSlotElement<T> = apply {
    element.slotStyle.isPlayerSlot(true)
}

/**
 * Extension: Accept quick move (shift-click)
 */
fun <T : ItemSlot> ItemSlotElement<T>.acceptQuickMove(priority: Int = 0): ItemSlotElement<T> = apply {
    element.slotStyle.acceptQuickMove(true)
    element.slotStyle.quickMovePriority(priority)
}

// ===========================
// XEI Integration Methods
// ===========================

/**
 * Extension: Mark as JEI/REI/EMI phantom slot
 */
fun <T : ItemSlot> ItemSlotElement<T>.asXeiPhantom(): ItemSlotElement<T> = apply {
    element.xeiPhantom()
}

/**
 * Extension: Mark as recipe ingredient for JEI/REI/EMI
 */
fun <T : ItemSlot> ItemSlotElement<T>.asXeiRecipeIngredient(io: IngredientIO): ItemSlotElement<T> = apply {
    element.xeiRecipeIngredient(io)
}

/**
 * Extension: Mark as recipe slot for JEI/REI/EMI
 */
fun <T : ItemSlot> ItemSlotElement<T>.asXeiRecipeSlot(io: IngredientIO = IngredientIO.NONE, chance: Float = 1f): ItemSlotElement<T> = apply {
    element.xeiRecipeSlot(io, chance)
}

