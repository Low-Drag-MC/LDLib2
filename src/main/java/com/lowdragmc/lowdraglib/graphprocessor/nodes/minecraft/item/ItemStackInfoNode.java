package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.item;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@LDLRegister(name = "itemstack info", group = "graph_processor.node.minecraft.item")
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
