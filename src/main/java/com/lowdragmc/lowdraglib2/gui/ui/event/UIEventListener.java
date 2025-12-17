package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;

import java.util.function.Consumer;

@FunctionalInterface
@KJSBindings
public interface UIEventListener extends Consumer<UIEvent> {
    @Override
    @Deprecated
    default void accept(UIEvent event) {
        handleEvent(event);
    }

    void handleEvent(UIEvent event);

    /**
     * Used for KJS
     */
    default UIEventListener create(UIEventListener eventListener) {
        return eventListener;
    }
}