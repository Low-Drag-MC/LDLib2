package com.lowdragmc.lowdraglib.graphprocessor.nodes.value;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.annotation.NumberColor;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;

@LDLRegister(name = "color", group = "graph_processor.node.value")
public class ColorNode extends BaseNode {
    @InputPort
    public Object in;
    @OutputPort
    public int out;

    @Configurable(showName = false)
    @NumberColor
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
