package com.lowdragmc.lowdraglib2.gui.util;

/**
 * The {@code ITickable} interface represents an entity that performs
 * a periodic action at regular intervals or upon invocation.
 * It can be implemented by classes that require a ticking mechanism,
 * such as game loops, schedulers, or timed updates.
 *
 * Implementing classes must define the {@code tick} method to specify
 * the action that should occur during each tick.
 *
 * @see #tick()
 */
public interface ITickable {
    void tick();
}
