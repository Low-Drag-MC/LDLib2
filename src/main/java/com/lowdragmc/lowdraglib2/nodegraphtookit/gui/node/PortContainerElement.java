package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class PortContainerElement extends ModelElement {
    public static final Predicate<PortModel> HORIZONTAL_PORT_FILTER = p -> p.getOrientation() == PortOrientation.Horizontal;
    public static final Predicate<PortModel> VERTICAL_PORT_FILTER = p -> p.getOrientation() == PortOrientation.Vertical;
    public static final Predicate<PortModel> INPUT_PORT_FILTER = p -> p.getDirection() == PortDirection.INPUT;
    public static final Predicate<PortModel> OUTPUT_PORT_FILTER = p -> p.getDirection() == PortDirection.OUTPUT;

    public final PortNodeModel portNodeModel;
    public final Predicate<PortModel> portFilter;
    // runtime
    @Getter @Nullable
    protected PortContainer portContainer;

    public PortContainerElement(PortNodeModel portNodeModel, Predicate<PortModel> portFilter) {
        this.portNodeModel = portNodeModel;
        this.portFilter = portFilter;
        addClass("__port-container__");
    }

    @Override
    protected void buildUI() {
        portContainer = createPortContainer();
        addChild(portContainer);
    }

    /**
     * Factory for the backing {@link PortContainer}. Overridden by vertical containers so the
     * spawned ports use the vertical layout.
     */
    protected PortContainer createPortContainer() {
        return new PortContainer();
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        var ports = portNodeModel.getPorts().stream().filter(portFilter).toList();
        if (portContainer != null) portContainer.updatePorts(visitor, ports, getGraphView());
    }
}
