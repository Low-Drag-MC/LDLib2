package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public interface IModularUIHolderMenu extends IModularUIHolder, IItemSlotHolderMenu {
    @Override
    default @Nullable ItemSlot getItemSlot(Slot slot) {
        return ldlib2$getItemSlot(slot);
    }

    @Override
    default @Nullable ModularUI getModularUI() {
        return ldlib2$getModularUI();
    }

    @Override
    default void addSlot(ItemSlot itemSlot) {
        IItemSlotHolderMenu.super.addSlot(itemSlot);
        ldlib2$addSlot(itemSlot);
    }

    default void setModularUI(ModularUI modularUI) {
        ldlib2$setModularUI(modularUI);
    }

    @Nullable ModularUI ldlib2$getModularUI();

    @Nullable ItemSlot ldlib2$getItemSlot(Slot slot);

    void ldlib2$addSlot(ItemSlot itemSlot);

    void ldlib2$setModularUI(ModularUI modularUI);
}
