package com.lowdragmc.lowdraglib2.gui.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.Event;

public abstract class ContainerMenuEvent extends Event {
    public final Player player;
    public final AbstractContainerMenu menu;
    protected ContainerMenuEvent(Player player, AbstractContainerMenu menu) {
        this.player = player;
        this.menu = menu;
    }

    /**
     * Event triggered when a new {@link AbstractContainerMenu} is created and associated with a {@link Player}.
     * This class is used to represent the creation process of a container menu,
     * allowing for further customization or handling through event listeners.
     *
     * <p><b>Usage:</b> This event can be posted to or consumed for adding additional logic when
     * a container menu is initialized for a player.</p>
     *
     * @see ContainerMenuEvent
     * @see AbstractContainerMenu
     * @see Player
     */
    public static class Create extends ContainerMenuEvent {
        public Create(Player player, AbstractContainerMenu menu) {
            super(player, menu);
        }

        /**
         * Return if the menu is created on the remote(client) side.
         */
        public boolean isRemote() {
            return player.level().isClientSide;
        }
    }
}
