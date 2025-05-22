package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.data;

import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

@LDLRegister(name = "compound", group = "graph_processor.node.minecraft.data", registry = "ldlib:graph_node")
public class CompoundNode extends BaseNode {
    @InputPort
    public Object in = null;
    @OutputPort
    public CompoundTag out = null;
    @Configurable(showName = false)
    public CompoundTag internalValue = new CompoundTag();

    @Override
    public int getMinWidth() {
        return 120;
    }

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
            return;
        } else if (in instanceof CompoundTag compoundTag) {
            out = compoundTag;
        } else {
            try {
                out = TagParser.parseTag(in.toString());
            } catch (CommandSyntaxException e) {
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
