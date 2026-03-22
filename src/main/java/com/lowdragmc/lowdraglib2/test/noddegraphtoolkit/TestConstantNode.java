package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import net.minecraft.network.chat.Component;

@NodeAttribute(name = "test_constant", group = "test", graphTypes = {TestGraph.class})
public class TestConstantNode extends Node {

    @Override
    public Component getDisplayName() {
        return Component.literal("Constant");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);
        context.addOption("value", Float.class)
                .withDefaultValue(10f);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        context.addOutputPort("out", Float.class)
                .build();
    }
}
