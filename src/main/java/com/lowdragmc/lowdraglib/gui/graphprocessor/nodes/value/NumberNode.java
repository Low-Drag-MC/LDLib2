package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.value;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;

@LDLRegister(name = "number", group = "graph_processor.node.value")
public class NumberNode extends BaseNode {
    @InputPort
    public Object in;
    @OutputPort
    public float out;

    @Configurable(showName = false)
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 1f)
    @DefaultValue(numberValue = {0})
    public float internalValue = 0;

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
            return;
        } else if (in instanceof Number number) {
            out = number.floatValue();
        } else if (in instanceof Boolean bool) {
            out = bool ? 1 : 0;
        } else {
            try {
                out = Float.parseFloat(in.toString());
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
