package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import org.joml.Vector2f;

public interface IMovable {
    /**
     * Get the model position
     */
    Vector2f getPosition();

    /**
     * Set the model position
     */
    void setPosition(Vector2f position);

    /**
     * Move the model by delta
     */
    void move(Vector2f delta);
}
