package com.lowdragmc.lowdraglib2.gui.ui.event;

import java.util.function.Consumer;

@FunctionalInterface
public interface UIEventListener extends Consumer<UIEvent> {
    @Override
    @Deprecated
    default void accept(UIEvent event) {
        handleEvent(event);
    }

    void handleEvent(UIEvent event);
}