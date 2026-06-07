package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.UseWithContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

@NodeAttribute(name = "test_preview_block", group = "test", graphTypes = {TestGraph.class})
@UseWithContext({TestContextNode.class})
public class TestPreviewBlock extends BlockNode {

    @Override
    public Component getDisplayName() {
        return Component.literal("PreviewBlock");
    }

    @Override
    public boolean hasNodePreview() {
        return true;
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        context.addInputPort("in", Float.class).withDefaultValue(0f).build();
        context.addOutputPort("out", Float.class).build();
    }
}
