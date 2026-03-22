package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import net.minecraft.network.chat.Component;

import java.util.Optional;

@NodeAttribute(name = "test_add", group = "test", graphTypes = {TestGraph.class})
public class TestAddNode extends Node {

    @Override
    public Component getDisplayName() {
        return Component.literal("Add");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);
        context.addOption("inputs", Integer.class)
                .withDefaultValue(2);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        Optional.ofNullable(getNodeOptionById("inputs")).ifPresent(o -> o.tryGetValue(Integer.class).ifSuccess(inputs -> {
            if (inputs instanceof Integer num) {
                for (var i = 0; i < num; i++) {
                    context.addInputPort("in" + (i + 1), Float.class);
                }
            }
        }));
        context.addOutputPort("out", Float.class);
    }
}
