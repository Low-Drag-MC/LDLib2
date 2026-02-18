package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

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
    }

    @Override
    protected void buildUI() {
        getLayout().flexDirection(FlexDirection.ROW);

        inputPortContainer = new PortContainer();
        inputPortContainer.getStyle().background(Sprites.RECT_LIGHT);
        outputPortContainer = new PortContainer();
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
