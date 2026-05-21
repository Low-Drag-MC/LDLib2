package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.InputOutputPortsNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Predicate;

public class InOutPortContainerElement extends PortContainerElement {
    @Getter
    @Nullable
    protected PortContainer inputPortContainer;
    @Getter
    @Nullable
    protected PortContainer outputPortContainer;

    public InOutPortContainerElement(PortNodeModel portNodeModel, Predicate<PortModel> portFilter) {
        super(portNodeModel, portFilter);
        addClass("__in-out-port-container__");
    }

    @Override
    protected void buildUI() {
        Style.defaultPipeline(getLayout(), l -> l.flexDirection(FlexDirection.ROW));

        inputPortContainer = new PortContainer();
        inputPortContainer.addClass("__in-out-port-container_input__");
        Style.defaultPipeline(inputPortContainer.getStyle(), s -> s.background(Sprites.RECT_LIGHT));
        outputPortContainer = new PortContainer();
        outputPortContainer.addClass("__in-out-port-container_output__");
        addChildren(inputPortContainer, outputPortContainer);
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        if (portNodeModel instanceof InputOutputPortsNodeModel portHolder) {
            var filteredPorts = new ArrayList<PortModel>();
            var anyVisible = false;
            var found = false;
            for (var port : portHolder.getVisibleInputsByDisplayOrder()) {
                if (portFilter.test(port)) {
                    if (port.isConnected()) {
                        anyVisible = true;
                    }
                    filteredPorts.add(port);
                    found = true;
                }
            }
            if (inputPortContainer != null) {
                inputPortContainer.updatePorts(visitor, filteredPorts, getGraphView());
            }
            filteredPorts.clear();
            found = false;
            for (var port : portHolder.getVisibleOutputsByDisplayOrder()) {
                if (portFilter.test(port)) {
                    if (port.isConnected()) {
                        anyVisible = true;
                    }
                    filteredPorts.add(port);
                    found = true;
                }
            }

            if (outputPortContainer != null) {
                outputPortContainer.updatePorts(visitor, filteredPorts, getGraphView());
            }

//            if (!portHolder.isCollapsible() || portHolder instanceof ICollapsible { Collapsed : true } collapsibleNode){
//                anyVisible = true;
//            }
        }

    }
}
