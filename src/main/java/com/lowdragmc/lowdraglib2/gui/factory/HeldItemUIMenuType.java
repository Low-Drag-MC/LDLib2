package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class HeldItemUIMenuType {
    /**
     * Opens a user interface (UI) for the specified player using the item held in the specified interaction hand.
     * This method checks if the held item can provide a {@link HeldItemUI} instance, creates a corresponding
     * UI holder, and attempts to open the menu for the player.
     *
     * @param player the {@link Player} attempting to open the UI
     * @param hand the {@link InteractionHand} used to interact, specifying which hand holds the item
     * @return {@code true} if the UI was opened successfully, {@code false} if the held item does not support
     *         a UI or the menu could not be opened
     */
    public static boolean openUI(ServerPlayer player, InteractionHand hand) {
        var heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof HeldItemUI heldItemUI) {
            var holder = heldItemUI.createUIHolder(player, hand, heldItem);
            return player.openMenu(holder).isPresent();
        }
        return false;
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inv, RegistryFriendlyByteBuf data) {
        var player = inv.player;
        var hand = data.readEnum(InteractionHand.class);
        var itemstack = ItemStack.OPTIONAL_STREAM_CODEC.decode(data);
        if (itemstack.getItem() instanceof HeldItemUI heldItemUI) {
            var holder = heldItemUI.createUIHolder(player, hand, itemstack);
            return new ModularUIContainerMenu(LDMenuTypes.HELD_ITEM_UI.get(), windowId, inv, holder);
        }
        throw new IllegalArgumentException("No held item ui found for item " + itemstack);
    }

    @FunctionalInterface
    public interface HeldItemUI {
        /**
         * Creates a {@code ModularUI} instance based on the provided {@link HeldItemUIHolder}.
         *
         * @param holder the {@link HeldItemUIHolder} containing contextual data
         *               required for generating the {@code ModularUI}
         * @return a {@code ModularUI} instance constructed using the*/
        ModularUI createUI(HeldItemUIHolder holder);

        /**
         * Creates a new instance of {@link HeldItemUIHolder} with the provided player, hand, and item stack.
         *
         * @param player The {@link Player} interacting with the UI.
         * @param hand The {@link InteractionHand} used by the player, specifying which hand is holding the item.
         * @param itemStack The {@link ItemStack} representing the item the player is interacting with.
         * @return A new {@link HeldItemUIHolder} instance initialized with the given context.
         */
        default HeldItemUIHolder createUIHolder(Player player, InteractionHand hand, ItemStack itemStack) {
            return new HeldItemUIHolder(this, player, hand, itemStack);
        }

        /**
         * Checks whether the given {@link HeldItemUIHolder} is still valid.
         * Validity is determined by comparing the {@link ItemStack} currently held in the specified player's hand
         * with the {@link ItemStack} stored in the {@link HeldItemUIHolder}.
         *
         * @param holder the {@link HeldItemUIHolder} which contains contextual information including the player,
         *               the hand being used, and the original {@link ItemStack}.
         * @return {@code true} if the current item held in the player's hand matches the stored {@link ItemStack},
         *         {@code false} otherwise.
         */
        default boolean stillValid(HeldItemUIHolder holder) {
            var current = holder.player.getItemInHand(holder.hand);
            return ItemStack.matches(current, holder.itemStack);
        }

        default Component getUIDisplayName(HeldItemUIHolder holder) {
            return Component.translatable(holder.itemStack.getDescriptionId());
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static class HeldItemUIHolder implements MenuProvider, IContainerUIHolder {
        public final HeldItemUI heldItemUI;
        public final Player player;
        public final InteractionHand hand;
        public final ItemStack itemStack;

        public HeldItemUIHolder(HeldItemUI heldItemUI, Player player, InteractionHand hand, ItemStack itemStack) {
            this.heldItemUI = heldItemUI;
            this.player = player;
            this.hand = hand;
            this.itemStack = itemStack;
        }

        @Override
        public boolean isStillValid(Player player) {
            return heldItemUI.stillValid(this);
        }

        @Override
        public Component getDisplayName() {
            return heldItemUI.getUIDisplayName(this);
        }

        @Override
        @Nullable
        public ModularUIContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ModularUIContainerMenu(LDMenuTypes.HELD_ITEM_UI.get(), containerId, playerInventory, this);
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            buffer.writeEnum(hand);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, itemStack);
        }

        @Override
        public ModularUI createUI(Player player) {
            return this.heldItemUI.createUI(this);
        }
    }
}
