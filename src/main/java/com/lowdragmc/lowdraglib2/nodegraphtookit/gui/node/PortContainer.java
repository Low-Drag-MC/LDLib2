package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortModelOptions;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortContainer extends UIElement {
    // runtime
    @Getter
    private List<PortModel> ports = Collections.emptyList();
    @Getter
    private Map<PortModel, PortElement> portElements = new HashMap<>();

    public PortContainer() {
        this.setId("node-port-container");
        this.getLayout().paddingAll(4).gapAll(2).flexGrow(1);
        this.getStyle().background(Sprites.RECT_SOLID);
    }

    public void updatePorts(ModelUpdateVisitor visitor, List<PortModel> ports, GraphView graphView) {
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
                element.updateElement(visitor);
                continue;
            }
            var portElement = new PortElement(port);
            portElement.setGraphView(graphView);
            portElement.doCompleteUpdate();
            addChildAt(portElement, index);
            portElements.put(port, portElement);
            index++;
        }
        this.setDisplay(!portElements.isEmpty());
    }
}
