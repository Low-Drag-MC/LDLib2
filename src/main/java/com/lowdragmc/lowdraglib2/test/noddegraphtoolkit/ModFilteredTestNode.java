package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

@NodeAttribute(
        name = "mod_filtered_test_node",
        group = "test",
        modID = "__missing_ldlib2_test_mod__",
        graphTypes = {TestGraph.class}
)
public class ModFilteredTestNode extends Node {
    @Override
    public Component getDisplayName() {
        return Component.literal("ModFiltered");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        context.addOutputPort("out", Integer.class).build();
    }
}
