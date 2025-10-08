package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.gui.sync.IUISyncManagerHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIContainerMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PlayerUIMenuType {
    private final static Map<ResourceLocation, Function<Player, PlayerUIHolder>> UI_HOLDERS = new ConcurrentHashMap<>();

    public static void register(ResourceLocation id, Function<Player, PlayerUIHolder> holder) {
        UI_HOLDERS.put(id, holder);
    }

    public static void unregister(ResourceLocation id) {
        UI_HOLDERS.remove(id);
    }

    /**
     * Opens a UI for the specified player if the given identifier is registered.
     * This method checks if a corresponding UI holder exists for the provided id,
     * creates the holder instance using the associated provider, and opens the menu for the player.
     *
     * @param player the {@link Player} for whom the UI should be opened
     * @param id the {@link ResourceLocation} identifier of the UI to be opened
     * @return {@code true} if the UI was successfully opened, {@code false} if the id is not registered
     *         or the holder instance could not be created
     */
    public static boolean openUI(Player player, ResourceLocation id) {
        if (!UI_HOLDERS.containsKey(id)) return false;
        var holder = UI_HOLDERS.get(id).apply(player);
        if (holder == null) return false;
        player.openMenu(holder);
        return true;
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inv, RegistryFriendlyByteBuf data) {
        var id = data.readResourceLocation();
        var holder = UI_HOLDERS.get(id).apply(inv.player);
        if (holder == null) throw new IllegalArgumentException("No player ui holder found for id " + id);
        var menu = new ModularUIContainerMenu(LDMenuTypes.PLAYER_UI.get(), windowId, inv, holder);
        menu.readInitialData(data);
        return menu;
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
        default ModularUIContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ModularUIContainerMenu(LDMenuTypes.PLAYER_UI.get(), containerId, playerInventory, this);
        }

        @Override
        default void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            buffer.writeResourceLocation(getUIId());
            if (menu instanceof IUISyncManagerHolder syncManagerHolder) {
                syncManagerHolder.writeInitialData(buffer);
            }
        }
    }
}
