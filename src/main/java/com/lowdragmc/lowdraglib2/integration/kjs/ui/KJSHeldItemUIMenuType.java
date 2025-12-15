package com.lowdragmc.lowdraglib2.integration.kjs.ui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

public class KJSHeldItemUIMenuType {
    public static boolean openUI(ServerPlayer player, InteractionHand hand, String id) {
        var heldItem = player.getItemInHand(hand);
        var event = new ItemUIEventJS(player, hand, heldItem, id);
        UIEvents.ITEM.post(ScriptType.SERVER, id, event);
        return player.openMenu(event).isPresent();
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inv, RegistryFriendlyByteBuf data) {
        var player = inv.player;
        var hand = data.readEnum(InteractionHand.class);
        var itemstack = ItemStack.OPTIONAL_STREAM_CODEC.decode(data);
        var id = data.readUtf();
        var event = new ItemUIEventJS(player, hand, itemstack, id);
        UIEvents.ITEM.post(ScriptType.CLIENT, id, event);
        return event.createMenu(windowId, inv, player);
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static class ItemUIEventJS extends UIEventJS {
        public final InteractionHand hand;
        public final ItemStack itemStack;

        public ItemUIEventJS(Player player, InteractionHand hand, ItemStack itemStack, String id) {
            super(player, id);
            this.hand = hand;
            this.itemStack = itemStack;

            // valid if still the same item
            this.validator = p -> ItemStack.matches(p.getItemInHand(hand), itemStack);
            this.displayName = itemStack.getHoverName();
        }

        @Override
        public MenuType<ModularUIContainerMenu> getMenuType() {
            return LDKJSMenuTypes.HELD_ITEM_UI.get();
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            buffer.writeEnum(hand);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, itemStack);
            super.writeClientSideData(menu, buffer);
        }
    }
}
