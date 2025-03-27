package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.item;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@LDLRegister(name = "itemstack", group = "graph_processor.node.minecraft.item")
public class ItemStackNode extends BaseNode {
    @InputPort
    public Object in;
    @InputPort
    public Item item;
    @InputPort
    public Integer count;
    @InputPort
    public DataComponentPatch components;
    @OutputPort(name = "itemstack")
    public ItemStack out = null;
    @Configurable(name = "itemstack", canCollapse = false, collapse = false)
    public ItemStack internalValue = new ItemStack(Items.AIR);

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public void process() {
        if (in == null) {
            out = internalValue.copy();
        } else if (in instanceof ItemStack itemStack){
            out = itemStack.copy();
        } else if (in instanceof CompoundTag itemTag) {
            out = ItemStack.parse(Platform.getFrozenRegistry(), itemTag).orElse(ItemStack.EMPTY);
        } else {
            out = new ItemStack(Items.AIR);
        }
        if (item != null) {
            var stack = new ItemStack(item, out.getCount());
            if (components != null && !out.isEmpty()) {
                out.applyComponents(components);
            }
            out = stack;
        }
        if (count != null) {
            out.setCount(count);
        }
        if (components != null && !out.isEmpty()) {
            out.applyComponents(components);
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
