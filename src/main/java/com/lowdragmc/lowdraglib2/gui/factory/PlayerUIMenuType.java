package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUIContainerMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerUIMenuType {
    private final static Map<ResourceLocation, PlayerUIHolder> UI_HOLDERS = new ConcurrentHashMap<>();

    public static void register(ResourceLocation id, PlayerUIHolder holder) {
        UI_HOLDERS.put(id, holder);
    }

    public static void unregister(ResourceLocation id) {
        UI_HOLDERS.remove(id);
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inv, RegistryFriendlyByteBuf data) {
        var id = data.readResourceLocation();
        var holder = UI_HOLDERS.get(id);
        if (holder == null) throw new IllegalArgumentException("No player ui holder found for id " + id);
        var syncManager = holder.createUISyncManager(inv.player);
        syncManager.readInitialData(data);
        return new ModularUIContainerMenu(LDMenuTypes.PLAYER_UI.get(), windowId, inv, holder, syncManager);
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public interface PlayerUIHolder extends MenuProvider, IContainerUIHolder {

        ResourceLocation getUIId();

        @Override
        default boolean isStillValid(Player player) {
            return true;
        }

        @Override
        default Component getDisplayName() {
            return Component.translatable(getUIId().toLanguageKey());
        }

        @Override
        @Nullable
        default AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ModularUIContainerMenu(LDMenuTypes.PLAYER_UI.get(), containerId, playerInventory, this, createUISyncManager(player));
        }

        @Override
        default void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            if (menu instanceof ModularUIContainerMenu modularUIContainerMenu) {
                modularUIContainerMenu.syncManager.writeInitialData(buffer);
            }
        }
    }
}
