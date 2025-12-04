package com.lowdragmc.lowdraglib2.gui.factory;


import com.lowdragmc.lowdraglib2.gui.sync.UISyncManager;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * It is used to create ui with {@link  ModularUIContainerMenu} and {@link  ModularUIContainerScreen}.
 * Which means UI used for remote and server.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IContainerUIHolder {
    /**
     * Creates a {@link ModularUI} instance tied to the specified {@link Player}
     * and managed through a {@link UISyncManager}.
     * This method is typically used to create a user interface for remote
     *
     * @param player      the {@link Player} for whom the UI is being created; represents
     *                    the user interacting with the interface.
     * @return the created {@link ModularUI} instance that handles the interface logic.
     */
    ModularUI createUI(Player player);

    /**
     * Determines if the UI is still valid for the specified player.
     */
    boolean isStillValid(Player player);
}
