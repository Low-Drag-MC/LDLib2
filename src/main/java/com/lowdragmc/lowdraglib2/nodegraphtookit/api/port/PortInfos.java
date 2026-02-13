package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.OrderedPorts;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PortInfos {
    public OrderedPorts portsById = new OrderedPorts();
    @Nullable
    public OrderedPorts previousPorts = null;
    public List<PortModel> orderedVisiblePorts = new ArrayList<>();
    public Map<String, PortModel> expandedPortsById = new HashMap<>();

    public void clear() {
        portsById = new OrderedPorts();
        previousPorts = null;
        orderedVisiblePorts.clear();
        expandedPortsById.clear();
    }
}
