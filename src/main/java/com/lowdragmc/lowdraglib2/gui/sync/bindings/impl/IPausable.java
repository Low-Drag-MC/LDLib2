package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

public interface IPausable {
    /**
     * Pauses the data source.
     */
    void pause();

    /**
     * Resumes the data source.
     */
    void resume();

    /**
     * Checks if the data source is paused.
     * @return true if the data source is paused, false otherwise.
     */
    boolean isPaused();

    /**
     * Toggles the pause state of the data source.
     */
    default void togglePause() {
        if (isPaused()) resume();
        else pause();
    }
}
