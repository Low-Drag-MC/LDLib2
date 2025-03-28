package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.data;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import net.minecraft.core.Direction;

@LDLRegister(name = "direction", group = "graph_processor.node.minecraft.data", registry = "ldlib:graph_node")
public class DirectionNode extends BaseNode {
    @InputPort
    public Object in = null;
    @OutputPort
    public Direction out = null;
    @Configurable(showName = false)
    public Direction internalValue = Direction.NORTH;

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
        } else if (in instanceof Direction direction) {
            out = direction;
        } else if (in instanceof Number number) {
            out = Direction.values()[number.intValue() % Direction.values().length];
        } else {
            try {
                out = Direction.valueOf(in.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                out = null;
            }
        }
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
