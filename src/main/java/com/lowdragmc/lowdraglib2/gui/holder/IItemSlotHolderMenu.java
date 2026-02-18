package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import org.jetbrains.annotations.Nullable;

public interface IItemSlotHolderMenu {

    default AbstractContainerMenu self() {
        return (AbstractContainerMenu) this;
    }

    /**
     * Adds a given {@link ItemSlot} to the menu. The {@code itemSlot} parameter is used to
     * retrieve its underlying {@link Slot} instance,
     * which is added internally to the menu.
     *
     * @param itemSlot the {@link ItemSlot} to be added to the menu
     */
    default void addSlot(ItemSlot itemSlot) {
        self().addSlot(itemSlot.getSlot());
    }

    /**
     * Retrieves the {@link ItemSlot} associated with the specified {@link Slot}.
     * If no {@link ItemSlot} corresponds to the given {@link Slot}, this method returns {@code null}.
     *
     * @param slot the {@link Slot} for which the associated {@link ItemSlot} is to be retrieved
     * @return the {@link ItemSlot} associated with the given {@link Slot}, or {@code null} if no association exists
     */
    @Nullable ItemSlot getItemSlot(Slot slot);

    default boolean isItemSlot(Slot slot) {
        return getItemSlot(slot) != null;
    }
}
