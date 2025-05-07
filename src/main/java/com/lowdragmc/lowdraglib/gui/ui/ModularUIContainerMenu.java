package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.core.mixins.accessor.AbstractContainerMenuAccessor;
import com.lowdragmc.lowdraglib.gui.util.PerTickIntCounter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ModularUIContainerMenu extends AbstractContainerMenu {
    public static final MenuType<ModularUIContainerMenu> MENUTYPE = new MenuType<>((i, inventory) -> new ModularUIContainerMenu(i), FeatureFlags.DEFAULT_FLAGS);

    public ModularUIContainerMenu(int windowID) {
        super(MENUTYPE, windowID);
    }

    //WARNING! WIDGET CHANGES SHOULD BE *STRICTLY* SYNCHRONIZED BETWEEN SERVER AND CLIENT,
    //OTHERWISE ID MISMATCH CAN HAPPEN BETWEEN ASSIGNED SLOTS!
    @Nonnull
    public Slot addSlot(@Nonnull Slot slotHandle) {
        var emptySlotIndex = slots.stream()
                .filter(it -> it instanceof EmptySlotPlaceholder)
                .mapToInt(slot -> slot.index).findFirst();
        if (emptySlotIndex.isPresent()) {
            slotHandle.index = emptySlotIndex.getAsInt();
            this.slots.set(slotHandle.index, slotHandle);
            ((AbstractContainerMenuAccessor)this).getLastSlots().set(slotHandle.index, ItemStack.EMPTY);
            ((AbstractContainerMenuAccessor)this).getRemoteSlots().set(slotHandle.index, ItemStack.EMPTY);
        } else {
            slotHandle.index = this.slots.size();
            this.slots.add(slotHandle);
            ((AbstractContainerMenuAccessor)this).getLastSlots().add(ItemStack.EMPTY);
            ((AbstractContainerMenuAccessor)this).getRemoteSlots().add(ItemStack.EMPTY);
        }
        return slotHandle;
    }

    //WARNING! WIDGET CHANGES SHOULD BE *STRICTLY* SYNCHRONIZED BETWEEN SERVER AND CLIENT,
    //OTHERWISE ID MISMATCH CAN HAPPEN BETWEEN ASSIGNED SLOTS!
    public void removeSlot(Slot slotHandle) {
        //replace removed slot with empty placeholder to avoid list index shift
        EmptySlotPlaceholder emptySlotPlaceholder = new EmptySlotPlaceholder();
        emptySlotPlaceholder.index = slotHandle.index;
        this.slots.set(slotHandle.index, emptySlotPlaceholder);
        ((AbstractContainerMenuAccessor)this).getLastSlots().set(slotHandle.index, ItemStack.EMPTY);
        ((AbstractContainerMenuAccessor)this).getRemoteSlots().set(slotHandle.index, ItemStack.EMPTY);
    }

    @Override
    public void removed(@Nonnull Player playerIn) {
        super.removed(playerIn);
    }

    @Override
    public void addSlotListener(@Nonnull ContainerListener pListener) {
        super.addSlotListener(pListener);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int dragType, @Nonnull ClickType clickTypeIn, @Nonnull Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            super.clicked(slotId, dragType, clickTypeIn, player);
        }
        if (slotId == -999) {
            super.clicked(slotId, dragType, clickTypeIn, player);
        }
    }

    private final PerTickIntCounter transferredPerTick = new PerTickIntCounter(0);

    public boolean canMergeSlot(Slot slot, ItemStack itemStack) {
        return slot.isActive();
    }

    private boolean isPlayerContainer(Slot slot) {
        return true;
    }

    private List<Slot> getShiftClickSlots(ItemStack itemStack, boolean fromContainer) {
        return this.slots.stream()
                .filter(slot -> canMergeSlot(slot, itemStack))
                .filter(slot -> isPlayerContainer(slot) == fromContainer)
                .sorted(Comparator.comparing(s -> (fromContainer ? -1 : 1) * this.slots.indexOf(s)))
                .collect(Collectors.toList());
    }

    public boolean attemptMergeStack(ItemStack itemStack, boolean fromContainer, boolean simulate) {
        return mergeItemStack(itemStack, getShiftClickSlots(itemStack, fromContainer), simulate);
    }

    public static boolean mergeItemStack(ItemStack itemStack, List<Slot> slots, boolean simulate) {
        if (itemStack.isEmpty())
            return false; //if we are merging empty stack, return

        boolean merged = false;
        //iterate non-empty slots first
        //to try to insert stack into them
        for (Slot slot : slots) {
            if (!slot.mayPlace(itemStack))
                continue; //if itemstack cannot be placed into that slot, continue
            ItemStack stackInSlot = slot.getItem();
            if (!ItemStack.isSameItem(itemStack, stackInSlot) || !ItemStack.isSameItemSameComponents(itemStack, stackInSlot))
                continue; //if itemstacks don't match, continue
            int slotMaxStackSize = Math.min(stackInSlot.getMaxStackSize(), slot.getMaxStackSize(stackInSlot));
            int amountToInsert = Math.min(itemStack.getCount(), slotMaxStackSize - stackInSlot.getCount());
            if (amountToInsert == 0)
                continue; //if we can't insert anything, continue
            //shrink our stack, grow slot's stack and mark slot as changed
            if (!simulate) {
                stackInSlot.grow(amountToInsert);
            }
            itemStack.shrink(amountToInsert);
            slot.setChanged();
            merged = true;
            if (itemStack.isEmpty())
                return true; //if we inserted all items, return
        }

        //then try to insert itemstack into empty slots
        //breaking it into pieces if needed
        for (Slot slot : slots) {
            if (!slot.mayPlace(itemStack))
                continue; //if itemstack cannot be placed into that slot, continue
            if (slot.hasItem())
                continue; //if slot contains something, continue
            int amountToInsert = Math.min(itemStack.getCount(), slot.getMaxStackSize(itemStack));
            if (amountToInsert == 0)
                continue; //if we can't insert anything, continue
            //split our stack and put result in slot
            ItemStack stackInSlot = itemStack.split(amountToInsert);
            if (!simulate) {
                slot.set(stackInSlot);
            }
            merged = true;
            if (itemStack.isEmpty())
                return true; //if we inserted all items, return
        }
        return merged;
    }


    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        if (!slot.hasItem()) {
            //return empty if we can't transfer it
            return ItemStack.EMPTY;
        }
        ItemStack stackInSlot = slot.getItem();
//        ItemStack stackToMerge = modularUI.getSlotMap().get(slot).onItemTake(player, stackInSlot.copy(), true);
        ItemStack stackToMerge = stackInSlot.copy();
        boolean fromContainer = !isPlayerContainer(slot);
        if (!attemptMergeStack(stackToMerge, fromContainer, true)) {
            return ItemStack.EMPTY;
        }
        int itemsMerged;
        if (stackToMerge.isEmpty() || canMergeSlot(slot, stackToMerge)) {
            itemsMerged = stackInSlot.getCount() - stackToMerge.getCount();
        } else {
            //if we can't have partial stack merge, we have to use all the stack
            itemsMerged = stackInSlot.getCount();
        }
        int itemsToExtract = itemsMerged;
        itemsMerged += transferredPerTick.get(player.level());
        if (itemsMerged > stackInSlot.getMaxStackSize()) {
            //we can merge at most one stack at a time
            return ItemStack.EMPTY;
        }
        transferredPerTick.increment(player.level(), itemsToExtract);
        //otherwise, perform extraction and merge
        ItemStack extractedStack = stackInSlot.split(itemsToExtract);
        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
//        extractedStack = modularUI.getSlotMap().get(slot).onItemTake(player, extractedStack, false);
        ItemStack resultStack = extractedStack.copy();
        if (!attemptMergeStack(extractedStack, fromContainer, false)) {
            resultStack = ItemStack.EMPTY;
        }
        if (!extractedStack.isEmpty()) {
            player.drop(extractedStack, false, false);
            resultStack = ItemStack.EMPTY;
        }
        return resultStack;
    }

    @Override
    public boolean canTakeItemForPickAll(@Nonnull ItemStack stack, @Nonnull Slot slotIn) {
        return canMergeSlot(slotIn, stack);
    }

    @Override
    public boolean stillValid(@Nonnull Player playerIn) {
        return true;
    }

    private static class EmptySlotPlaceholder extends Slot {

        private static final Container EMPTY_INVENTORY = new SimpleContainer(0);

        public EmptySlotPlaceholder() {
            super(EMPTY_INVENTORY, 0, -100000, -100000);
        }

        @Nonnull
        @Override
        public ItemStack getItem() {
            return ItemStack.EMPTY;
        }
        
        
        @Override
        public void set(@Nonnull ItemStack stack) {
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@Nonnull Player playerIn) {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }
}
