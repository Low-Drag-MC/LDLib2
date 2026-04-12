package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PlayerUIMenuType {
    private final static Map<ResourceLocation, Function<Player, PlayerUIHolder>> UI_HOLDERS = new ConcurrentHashMap<>();

    public static void register(ResourceLocation id, Function<Player, PlayerUIHolder> holder) {
        UI_HOLDERS.put(id, holder);
    }

    public static void unregister(ResourceLocation id) {
        UI_HOLDERS.remove(id);
    }

    public static boolean openUI(Player player, ResourceLocation id) {
        if (!UI_HOLDERS.containsKey(id)) return false;
        var holder = UI_HOLDERS.get(id).apply(player);
        if (holder == null) return false;
        player.openMenu(new ExtendedScreenHandlerFactory<ResourceLocation>() {
            @Override
            public ResourceLocation getScreenOpeningData(ServerPlayer player) {
                return id;
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable(id.toLanguageKey());
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new ModularUIContainerMenu(LDMenuTypes.PLAYER_UI, containerId, playerInventory, holder);
            }
        });
        return true;
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inv, net.minecraft.resources.ResourceLocation id) {
        var holder = UI_HOLDERS.get(id).apply(inv.player);
        if (holder == null) throw new IllegalArgumentException("No player ui holder found for id " + id);
        return new ModularUIContainerMenu(LDMenuTypes.PLAYER_UI, windowId, inv, holder);
    }

    @FunctionalInterface
    public interface PlayerUIHolder extends IContainerUIHolder {
        @Override
        default boolean isStillValid(Player player) {
            return true;
        }
    }
}
