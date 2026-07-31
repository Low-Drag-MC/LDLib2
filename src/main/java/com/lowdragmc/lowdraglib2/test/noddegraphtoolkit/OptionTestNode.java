package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

@NodeAttribute(name = "option_test_node", group = "test", graphTypes = {TestGraph.class})
public class OptionTestNode extends Node {
    @Override
    public Component getDisplayName() {
        return Component.literal("Option Test");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);
        // with authored tooltips: the configurator gets a "?" tip icon after it
        context.addOption("enum", Direction.class).withDefaultValue(Direction.WEST)
                .withTooltips(Tooltips.of(Component.literal("Single line tooltip")));
        context.addOption("string[]", String[].class).withDefaultValue(new String[] {"Hello World!"})
                .withTooltips(Tooltips.of(
                        Component.literal("First tooltip line"),
                        Component.literal("Second tooltip line")));
        // without tooltips: no tip icon at all
        context.addOption("color", TypeHandles.COLOR);
        context.addOption("block", Block.class);
        context.addOption("stack", ItemStack.class);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        context.addInputPort("in", TypeHandles.DIRECTION).build();
        context.addOutputPort("out", Direction.class).build();
    }
}
