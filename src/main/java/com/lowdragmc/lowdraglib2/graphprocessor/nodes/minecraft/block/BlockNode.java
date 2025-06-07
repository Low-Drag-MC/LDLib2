package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.block;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@LDLRegister(name = "block", group = "graph_processor.node.minecraft.block", registry = "ldlib2:graph_node")
public class BlockNode extends BaseNode {
    @InputPort
    public Object in = null;
    @OutputPort
    public Block out = null;
    @Configurable(showName = false)
    public Block internalValue = Blocks.AIR;

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
            return;
        } else if (in instanceof Block block) {
            out = block;
        } else if (in instanceof BlockState blockState) {
            out = blockState.getBlock();
        } else {
            var name = in.toString();
            if (LDLib2.isValidResourceLocation(name)) {
                out = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
            } else {
                out = null;
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
