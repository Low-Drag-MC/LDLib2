package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an item in a contextual (right-click) menu.
 *
 * <p>TODO: Implement contextual menu system for your UI framework.</p>
 */
public class ContextualMenuItem {

    /**
     * -- GETTER --
     *  Gets the display name of the item.
     *
     * @return the name
     */
    @Getter
    private final String name;
    /**
     * -- GETTER --
     *  Gets the priority of the item.
     *
     * @return the priority
     */
    @Getter
    private final int priority;
    private final Runnable action;

    /**
     * Creates a new contextual menu item.
     *
     * @param name the display name of the item
     * @param priority the priority (order) of the item
     */
    public ContextualMenuItem(String name, int priority) {
        this(name, priority, null);
    }

    /**
     * Creates a new contextual menu item with an action.
     *
     * @param name the display name of the item
     * @param priority the priority (order) of the item
     * @param action the action to perform when the item is clicked
     */
    public ContextualMenuItem(String name, int priority, Runnable action) {
        this.name = name;
        this.priority = priority;
        this.action = action;
    }

    /**
     * Creates a contextual menu item from another item.
     *
     * @param other the other item to copy
     * @param priority the new priority
     */
    public ContextualMenuItem(ContextualMenuItem other, int priority) {
        this.name = other.name;
        this.priority = priority;
        this.action = other.action;
    }

    /**
     * Gets the action to perform when the item is clicked.
     *
     * @return the action, or {@code null} if none
     */
    public @Nullable Runnable getAction() {
        return action;
    }

    /**
     * Returns a copy of this item with the given action bound.
     *
     * @param action the action to bind
     * @return a new item with the same name and priority but with the given action
     */
    public ContextualMenuItem withAction(Runnable action) {
        return new ContextualMenuItem(this.name, this.priority, action);
    }

    /**
     * Executes the action if one is defined.
     */
    public void execute() {
        if (action != null) {
            action.run();
        }
    }
}
