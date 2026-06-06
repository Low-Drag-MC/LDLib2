package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortModelOptions;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import dev.vfyjxf.taffy.style.TaffyDisplay;
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
        addClass("__node-port-container__");
        Style.defaultPipeline(getLayout(), l -> l.paddingAll(4).gapAll(2).flexGrow(1));
        Style.defaultPipeline(getStyle(), s -> s.background(Sprites.RECT_SOLID));
    }

    /**
     * Factory for the per-port element. Overridden by vertical containers to spawn a
     * {@link VerticalPortElement} instead of the default horizontal {@link PortElement}.
     */
    protected PortElement createPortElement(PortModel port) {
        return new PortElement(port);
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
            var portElement = createPortElement(port);
            portElement.setGraphView(graphView);
            portElement.doCompleteUpdate();
            addChildAt(portElement, index);
            portElements.put(port, portElement);
            index++;
        }
        // Hide empty container — data-driven by how many ports we ended up showing.
        Style.importantPipeline(getLayout(), l -> l.display(portElements.isEmpty() ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
    }
}
