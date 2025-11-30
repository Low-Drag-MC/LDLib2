package com.lowdragmc.lowdraglib2.integration.kjs.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUIContainerMenu;
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
        var menu = event.createMenu(windowId, inv, player);
        if (menu == null) return null;
        menu.readInitialData(data);
        return menu;
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
