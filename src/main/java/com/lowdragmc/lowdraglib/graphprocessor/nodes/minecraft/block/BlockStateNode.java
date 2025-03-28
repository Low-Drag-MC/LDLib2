package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.block;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@LDLRegister(name = "blockstate", group = "graph_processor.node.minecraft.block", registry = "ldlib:graph_node")
public class BlockStateNode extends BaseNode {
    @InputPort
    public Object in = null;
    @OutputPort
    public BlockState out = null;
    @Configurable(name = "blockstate", canCollapse = false, collapse = false)
    public BlockState internalValue = Blocks.AIR.defaultBlockState();

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
            return;
        } else if (in instanceof BlockState state) {
            out = state;
        } else if (in instanceof Block block) {
            out = block.defaultBlockState();
        } else if (in instanceof CompoundTag tag) {
            out = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
        } else {
            var name = in.toString();
            if (LDLib.isValidResourceLocation(name)) {
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
                out = block.defaultBlockState();
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
