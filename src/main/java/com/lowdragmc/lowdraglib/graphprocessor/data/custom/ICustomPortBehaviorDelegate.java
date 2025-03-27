package com.lowdragmc.lowdraglib.graphprocessor.data.custom;

import com.lowdragmc.lowdraglib.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib.graphprocessor.data.PortEdge;

import java.util.List;

public interface ICustomPortBehaviorDelegate {
    /**
     * Return the new list of port data by the current port edges
     */
    List<PortData> handle(List<PortEdge> edges);
}
