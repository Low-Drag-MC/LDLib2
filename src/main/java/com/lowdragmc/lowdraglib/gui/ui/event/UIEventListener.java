package com.lowdragmc.lowdraglib.gui.ui.event;

@FunctionalInterface
public interface UIEventListener {
    void handleEvent(UIEvent event);
}