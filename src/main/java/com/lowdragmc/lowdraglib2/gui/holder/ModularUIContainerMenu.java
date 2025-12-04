package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUIContainerMenu extends AbstractContainerMenu {
    public final Inventory inventory;
    public final IContainerUIHolder uiHolder;
    @Getter
    public final ModularUI modularUI;

    public ModularUIContainerMenu(MenuType<ModularUIContainerMenu> menuType,
                                  int windowID,
                                  Inventory inventory,
                                  IContainerUIHolder uiHolder) {
        super(menuType, windowID);
        this.inventory = inventory;
        this.uiHolder = uiHolder;
        this.modularUI = uiHolder.createUI(inventory.player);
        asModularUIHolderMenu().setModularUI(modularUI);
    }

    public IModularUIHolderMenu asModularUIHolderMenu() {
        return (IModularUIHolderMenu) this;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        if (player.level().isClientSide) {
            return ItemStack.EMPTY;
        }

        var clickSlot = this.slots.get(idx);

        // Vanilla will check this before calling this method, but we do use it in other contexts as well (move region)
        if (!clickSlot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        var stackToMove = clickSlot.getItem();
        if (stackToMove.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var originalStackToMove = stackToMove.copy();

        stackToMove = performQuickMoveStack(stackToMove, isPlayerSideSlot(clickSlot));

        // While we did modify stackToMove in-place, this causes the container to be notified of the change
        if (!ItemStack.matches(originalStackToMove, stackToMove)) {
            clickSlot.setByPlayer(stackToMove.isEmpty() ? ItemStack.EMPTY : stackToMove);
        }

        return ItemStack.EMPTY;
    }

    protected ItemStack performQuickMoveStack(ItemStack stackToMove, boolean fromPlayerSide) {
        // Allow moving items from player-side slots into some "remote" inventory that is not slot-based
        // This is used to move items into the network inventory
        if (fromPlayerSide) {
//            stackToMove = this.transferStackToMenu(stackToMove);
            if (stackToMove.isEmpty()) {
                return stackToMove;
            }
        }

        var destinationSlots = getQuickMoveDestinationSlots(stackToMove, fromPlayerSide);

        // If no actual targets were available, allow moving into filter slots too
        if (destinationSlots.isEmpty() && fromPlayerSide) {
            // TODO FakeSlot
//            for (Slot cs : this.slots) {
//                if (cs instanceof FakeSlot && !isPlayerSideSlot(cs)) {
//                    var destination = cs.getItem();
//                    if (ItemStack.isSameItemSameComponents(destination, stackToMove)) {
//                        break; // Item is already in the filter
//                    } else if (destination.isEmpty()) {
//                        cs.set(stackToMove.copy());
//                        // ???
//                        this.broadcastChanges();
//                        break;
//                    }
//                }
//            }
            return stackToMove; // Since destinationSlots was empty, nothing else to do
        }

        // Try stacking the item into filled slots first
        for (var dest : destinationSlots) {
            if (dest.hasItem() && (stackToMove = dest.safeInsert(stackToMove)).isEmpty()) {
                return stackToMove;
            }
        }

        // Now try placing it in empty slots, if it's not already fully consumed
        for (var dest : destinationSlots) {
            if (!dest.hasItem() && (stackToMove = dest.safeInsert(stackToMove)).isEmpty()) {
                return stackToMove;
            }
        }

        return stackToMove;
    }

    protected List<Slot> getQuickMoveDestinationSlots(ItemStack stackToMove, boolean fromPlayerSide) {
        // Find potential destination slots
        var destinationSlots = new ArrayList<Slot>();
        for (var candidateSlot : this.slots) {
            if (isValidQuickMoveDestination(candidateSlot, stackToMove, fromPlayerSide)) {
                destinationSlots.add(candidateSlot);
            }
        }

        // Order slots by the priority of their semantic
        destinationSlots.sort(Comparator.comparingInt(this::getQuickMovePriority).reversed());
        return destinationSlots;
    }

    protected int getQuickMovePriority(Slot slot) {
        var itemSlot = asModularUIHolderMenu().getItemSlot(slot);
        if (itemSlot == null) {
            return 0;
        }
        return itemSlot.getSlotStyle().quickMovePriority();
    }

    /**
     * Check if a given candidate slot is a valid destination for {@link #quickMoveStack}.
     */
    protected boolean isValidQuickMoveDestination(Slot candidateSlot, ItemStack stackToMove,
                                                  boolean fromPlayerSide) {
        var itemSlot = asModularUIHolderMenu().getItemSlot(candidateSlot);
        return isPlayerSideSlot(candidateSlot) != fromPlayerSide
                && (itemSlot == null || itemSlot.getSlotStyle().acceptQuickMove())
                && candidateSlot.mayPlace(stackToMove);
    }

    /**
     * Check if a given slot is considered to be "on the player side" for the purposes of shift-clicking items back and
     * forth between the opened menu and the player's inventory.
     */
    protected boolean isPlayerSideSlot(Slot slot) {
        if (slot.container == inventory) {
            return true;
        }

        var itemSlot = asModularUIHolderMenu().getItemSlot(slot);
        return itemSlot != null && itemSlot.getSlotStyle().isPlayerSlot();
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return uiHolder.isStillValid(playerIn);
    }
}
