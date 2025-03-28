package com.lowdragmc.lowdraglib.graphprocessor.nodes.value;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;

@LDLRegister(name = "bool", group = "graph_processor.node.value", registry = "ldlib:graph_node")
public class BoolNode extends BaseNode {
    @InputPort
    public Object in = null;
    @OutputPort
    public boolean out = false;
    @Configurable(showName = false)
    public boolean internalValue = false;

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
            return;
        } else if (in instanceof Boolean) {
            out = (boolean) in;
        } else if (in instanceof Number number) {
            out = number.floatValue() != 0;
        } else {
            out = Boolean.parseBoolean(in.toString());
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
