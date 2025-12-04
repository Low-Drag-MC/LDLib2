package com.lowdragmc.lowdraglib2.integration.kjs.ui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import javax.annotation.ParametersAreNonnullByDefault;

public class KJSPlayerUIMenuType {
    public static boolean openUI(ServerPlayer player, String id) {
        var event = new PlayerUIEventJS(player, id);
        UIEvents.PLAYER.post(ScriptType.SERVER, id, event);
        return player.openMenu(event).isPresent();
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inv, RegistryFriendlyByteBuf data) {
        var player = inv.player;
        var id = data.readUtf();
        var event = new PlayerUIEventJS(player, id);
        UIEvents.PLAYER.post(ScriptType.CLIENT, id, event);
        return event.createMenu(windowId, inv, player);
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static class PlayerUIEventJS extends UIEventJS {
        public PlayerUIEventJS(Player player, String id) {
            super(player, id);
        }

        @Override
        public MenuType<ModularUIContainerMenu> getMenuType() {
            return LDKJSMenuTypes.PLAYER_UI.get();
        }
    }
}
