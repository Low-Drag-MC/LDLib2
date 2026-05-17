package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.UseWithContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

@NodeAttribute(name = "test_block_b", group = "test", graphTypes = {TestGraph.class})
@UseWithContext({TestContextNode.class})
public class TestBlockB extends BlockNode {

    @Override
    public Component getDisplayName() {
        return Component.literal("BlockB");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        context.addInputPort("inA", Float.class);
        context.addInputPort("inB", Float.class);
        context.addOutputPort("out", Float.class);
    }
}
