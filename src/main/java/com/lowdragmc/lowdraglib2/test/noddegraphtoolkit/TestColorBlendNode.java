package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

@NodeAttribute(name = "test_color_blend", group = "test", graphTypes = {TestGraph.class})
public class TestColorBlendNode extends Node {

    @Override
    public Component getDisplayName() {
        return Component.literal("Color Blend");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        context.addInputPort("a", TypeHandles.COLOR)
                .build();
        context.addInputPort("b", TypeHandles.COLOR)
                .build();
        context.addOutputPort("out", TypeHandles.COLOR)
                .build();
    }
}
