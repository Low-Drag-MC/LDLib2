package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.block;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

@LDLRegister(name = "blockstate info", group = "graph_processor.node.minecraft.block", registry = "ldlib2:graph_node")
public class BlockStateInfoNode extends BaseNode {
    @InputPort
    public BlockState in = null;
    @InputPort
    public String property = null;
    @OutputPort
    public String value = null;
    @Configurable(name = "property")
    public String internalKey = "";

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public void process() {
        value = null;
        if (in != null) {
            var propertyKey = property == null ? internalKey : property;
            Property p = in.getBlock().getStateDefinition().getProperty(propertyKey);
            if (p != null) {
                value = p.getName(in.getValue(p));
            }
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("key")) {
                if (!port.getEdges().isEmpty()) return;
            }
        }
        super.buildConfigurator(father);
    }
}
