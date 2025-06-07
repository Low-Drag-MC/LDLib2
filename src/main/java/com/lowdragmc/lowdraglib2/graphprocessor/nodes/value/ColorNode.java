package com.lowdragmc.lowdraglib2.graphprocessor.nodes.value;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

@LDLRegister(name = "color", group = "graph_processor.node.value", registry = "ldlib2:graph_node")
public class ColorNode extends BaseNode {
    @InputPort
    public Object in;
    @OutputPort
    public int out;

    @Configurable(showName = false)
    @ConfigColor
    @DefaultValue(numberValue = -1)
    public int internalValue = -1;

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
            return;
        } else if (in instanceof Number number) {
            out = number.intValue();
        } else {
            try {
                out = Integer.parseInt(in.toString());
            } catch (NumberFormatException e) {
                out = 0;
            }
        }
        internalValue = out;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("in")) {
                if (!port.getEdges().isEmpty()) return;
            }
        }
        super.buildConfigurator(father);
    }
}
