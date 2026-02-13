package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortModelOptions;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortContainer extends UIElement {
    public final NodeElement nodeElement;

    // runtime
    @Getter
    private List<PortModel> ports = Collections.emptyList();
    @Getter
    private Map<PortModel, PortElement> portElements = new HashMap<>();

    public PortContainer(NodeElement nodeElement) {
        this.nodeElement = nodeElement;
    }

    public void loadPorts(List<PortModel> ports) {
        var previousPorts = this.ports;
        this.ports = List.copyOf(ports);
        // remove outdated elements
        for (var portModel : previousPorts) {
            if (ports.contains(portModel) || portModel.getOptions().hasFlag(PortModelOptions.HIDDEN)) continue;
            var ele = portElements.remove(portModel);
            if (ele != null) {
                ele.removeSelf();
            }
        }
        // add new elements
        var index = 0;
        for (PortModel port : ports) {
            if (port.getOptions().hasFlag(PortModelOptions.HIDDEN)) continue;
            var element = portElements.get(port);
            if (element != null) {
                // reorder
                if (element.getSiblingIndex() != index) {
                    removeChild(element);
                    addChildAt(element, index);
                }
                index++;
                continue;
            }
            var portElement = new PortElement(port);
            portElement.setGraphView(nodeElement.getGraphView());
            addChildAt(portElement, index);
            portElements.put(port, portElement);
            index++;
        }
        this.setDisplay(!portElements.isEmpty());
    }
}
