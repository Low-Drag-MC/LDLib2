package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.item;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@LDLRegister(name = "itemstack info", group = "graph_processor.node.minecraft.item", registry = "ldlib2:graph_node")
public class ItemStackInfoNode extends BaseNode {
    @InputPort
    public ItemStack in = null;
    @OutputPort
    public Item out = null;
    @OutputPort
    public int count = 0;
    @OutputPort
    public DataComponentMap components;

    @Override
    public void process() {
        out = null;
        count = 0;
        components = null;
        if (in != null) {
            out = in.getItem();
            count = in.getCount();
            components = in.getComponents();
        }
    }

}
