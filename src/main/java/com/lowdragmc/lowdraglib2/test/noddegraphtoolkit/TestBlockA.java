package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.UseWithContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.Optional;

@NodeAttribute(name = "test_block_a", group = "test", graphTypes = {TestGraph.class})
@UseWithContext({TestContextNode.class})
public class TestBlockA extends BlockNode {

    @Override
    public Component getDisplayName() {
        return Component.literal("BlockA");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);
        context.addOption("enum", Direction.class).withDefaultValue(Direction.WEST);
        context.addOption("inputs", Integer.class)
                .withDefaultValue(2);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        Optional.ofNullable(getNodeOptionById("inputs")).ifPresent(o -> o.tryGetValue(Integer.class).ifSuccess(inputs -> {
            if (inputs instanceof Integer num) {
                for (var i = 0; i < num; i++) {
                    context.addInputPort("in" + (i + 1), String.class);
                }
            }
        }));
        context.addOutputPort("out", Float.class);
    }
}
