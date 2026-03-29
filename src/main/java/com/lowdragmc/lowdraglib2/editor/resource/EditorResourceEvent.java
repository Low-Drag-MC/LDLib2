package com.lowdragmc.lowdraglib2.editor.resource;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface EditorResourceEvent {
    Event<LoadBuiltin> LOAD_BUILTIN = EventFactory.createArrayBacked(LoadBuiltin.class,
            (listeners) -> (resourceInstance) -> {
                for (LoadBuiltin listener : listeners) {
                    listener.onLoad(resourceInstance);
                }
            });

    @FunctionalInterface
    interface LoadBuiltin {
        void onLoad(ResourceInstance<?> resourceInstance);
    }
}
