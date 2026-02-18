package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2f;

public class PlaceholderData {
    @Getter @Setter
    private String groupName = null;
    @Getter @Setter
    Vector2f position = new Vector2f();
}
