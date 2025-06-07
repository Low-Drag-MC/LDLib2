package com.lowdragmc.lowdraglib2.graphprocessor.data.custom;

import com.lowdragmc.lowdraglib2.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib2.graphprocessor.data.PortEdge;

import java.util.List;

public interface ICustomPortBehaviorDelegate {
    /**
     * Return the new list of port data by the current port edges
     */
    List<PortData> handle(List<PortEdge> edges);
}
