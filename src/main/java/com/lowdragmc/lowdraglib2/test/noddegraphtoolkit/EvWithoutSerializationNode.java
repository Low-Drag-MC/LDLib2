package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

/** Save-or-load schema: Float port with explicit {@code withoutSerialization()}, default 42.0f. */
@NodeAttribute(name = "ev_without_serialization", group = "test", graphTypes = {TestGraph.class})
public class EvWithoutSerializationNode extends Node {
    @Override public Component getDisplayName() { return Component.literal("Ev Without Serialization"); }
    @Override public void onDefinePorts(IPortDefinitionContext context) {
        var b = context.addInputPort("port", Float.class);
        b.withoutSerialization();
        b.withDefaultValue(42.0f);
        b.build();
        context.addOutputPort("out", Float.class).build();
    }
}
