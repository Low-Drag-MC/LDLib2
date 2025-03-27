package com.lowdragmc.lowdraglib.graphprocessor.data.custom;

import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.graphprocessor.data.PortEdge;

import java.util.List;

public interface ICustomPortIODelegate {
    /**
     * Push / pull the data from the input port to the output port.
     */
    void handle(BaseNode node, List<PortEdge> edges, NodePort port);
}
