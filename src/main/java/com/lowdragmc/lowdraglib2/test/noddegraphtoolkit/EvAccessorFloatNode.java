package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

/** Save-side schema for evolution tests: Float port via standard accessor, builder default 7.0f. */
@NodeAttribute(name = "ev_accessor_float", group = "test", graphTypes = {TestGraph.class})
public class EvAccessorFloatNode extends Node {
    @Override public Component getDisplayName() { return Component.literal("Ev Accessor Float"); }
    @Override public void onDefinePorts(IPortDefinitionContext context) {
        var b = context.addInputPort("port", Float.class);
        b.withDefaultValue(7.0f);
        b.build();
        context.addOutputPort("out", Float.class).build();
    }
}
