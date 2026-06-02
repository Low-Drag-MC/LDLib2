package com.lowdragmc.lowdraglib2.editor.resource;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public abstract class EditorResourceEvent extends Event implements IModBusEvent {
    public final ResourceInstance<?> resourceInstance;

    public EditorResourceEvent(ResourceInstance<?> resourceInstance) {
        this.resourceInstance = resourceInstance;
    }

    public static class LoadBuiltin extends EditorResourceEvent {
        public <T> LoadBuiltin(ResourceInstance<T> resourceInstance) {
            super(resourceInstance);
        }
    }
}
