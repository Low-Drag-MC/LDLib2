package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

/** No {@code @UseWithContext} — used to verify the compatibility check rejects insertion. */
@NodeAttribute(name = "test_unrelated_block", group = "test", graphTypes = {TestGraph.class})
public class TestUnrelatedBlock extends BlockNode {

    @Override
    public Component getDisplayName() {
        return Component.literal("UnrelatedBlock");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        context.addInputPort("in", Float.class);
    }
}
