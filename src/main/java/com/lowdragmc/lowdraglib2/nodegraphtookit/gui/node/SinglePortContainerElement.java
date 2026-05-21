package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class SinglePortContainerElement extends ModelElement {
    public static final Predicate<PortModel> HORIZONTAL_PORT_FILTER = p -> p.getOrientation() == PortOrientation.Horizontal;
    public static final Predicate<PortModel> VERTICAL_PORT_FILTER = p -> p.getOrientation() == PortOrientation.Vertical;
    public static final Predicate<PortModel> INPUT_PORT_FILTER = p -> p.getDirection() == PortDirection.INPUT;
    public static final Predicate<PortModel> OUTPUT_PORT_FILTER = p -> p.getDirection() == PortDirection.OUTPUT;

    public final PortModel portModel;
    // runtime
    @Getter @Nullable
    protected PortContainer portContainer;
    @Getter @Nullable
    protected PortElement portElement;

    public SinglePortContainerElement(PortModel portModel) {
        this.portModel = portModel;
        addClass("__single-port-container__");
    }

    @Override
    protected void buildUI() {
        portContainer = new PortContainer();
        addChild(portContainer);
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        if (portContainer != null) {
            portContainer.updatePorts(visitor, List.of(portModel), getGraphView());
            portElement = portContainer.getPortElements().get(portModel);
        } else {
            portElement = null;
        }
    }
}
