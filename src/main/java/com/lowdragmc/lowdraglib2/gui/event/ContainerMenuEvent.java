package com.lowdragmc.lowdraglib2.gui.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface ContainerMenuEvent {
    Event<Create> CREATE = EventFactory.createArrayBacked(Create.class, callbacks -> (player, menu) -> {
        for (Create callback : callbacks) {
            callback.onCreate(player, menu);
        }
    });

    interface Create {
        void onCreate(Player player, AbstractContainerMenu menu);
    }
}

