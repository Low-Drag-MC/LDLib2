package com.lowdragmc.lowdraglib2.graphprocessor.data;

import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Class that describe port attributes for it's creation
 */
@Setter
@Accessors(chain = true, fluent = true)
public class PortData {
    /**
     * Display name on the node
     */
    public String displayName = "";
    /**
     * port color
     */
    public int portColor = 0; // 0 - auto color
    /**
     * Unique identifier for the port
     */
    @Nullable
    public String identifier;
    /**
     * he type that will be used for coloring with the type stylesheet
     */
    @Nullable
    public Class displayType;
    /**
     * If the port accept multiple connection
     */
    public boolean acceptMultipleEdges;
    /**
     * Tooltip of the port
     */
    @Nullable
    public List<String> tooltip;

    public PortData() {
    }

    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof PortData  other)
            return Objects.equals(identifier, other.identifier)
                    && Objects.equals(displayName, other.displayName)
                    && portColor == other.portColor
                    && displayType == other.displayType
                    && acceptMultipleEdges == other.acceptMultipleEdges
                    && Objects.equals(tooltip, other.tooltip);
        return false;
    }

    public void CopyFrom(PortData other) {
        identifier = other.identifier;
        portColor = other.portColor;
        displayName = other.displayName;
        displayType = other.displayType;
        acceptMultipleEdges = other.acceptMultipleEdges;
        tooltip = other.tooltip;
    }
}
