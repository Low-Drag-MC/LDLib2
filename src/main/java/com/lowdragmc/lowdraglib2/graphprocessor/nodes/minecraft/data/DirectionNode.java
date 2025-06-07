package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.data;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import net.minecraft.core.Direction;

@LDLRegister(name = "direction", group = "graph_processor.node.minecraft.data", registry = "ldlib2:graph_node")
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
