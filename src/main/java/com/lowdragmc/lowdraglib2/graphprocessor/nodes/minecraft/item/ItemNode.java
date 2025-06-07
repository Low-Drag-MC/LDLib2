package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.item;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@LDLRegister(name = "item", group = "graph_processor.node.minecraft.item", registry = "ldlib2:graph_node")
public class ItemNode extends BaseNode {
    @InputPort
    public Object in = null;
    @OutputPort
    public Item out = null;
    @Configurable(showName = false)
    public Item internalValue = Items.AIR;

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
            return;
        } else if (in instanceof Item item) {
            out = item;
        } else if (in instanceof ItemStack itemStack) {
            out = itemStack.getItem();
        } else {
            var name = in.toString();
            if (LDLib2.isValidResourceLocation(name)) {
                out = BuiltInRegistries.ITEM.get(ResourceLocation.parse(name));
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
