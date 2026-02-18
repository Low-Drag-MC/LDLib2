package com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire;

import org.joml.Vector2f;

public interface IGhostWireModel {
    Vector2f getFromWorldPoint();
    Vector2f getToWorldPoint();
    void setFromWorldPoint(Vector2f fromWorldPoint);
    void setToWorldPoint(Vector2f toWorldPoint);
}
