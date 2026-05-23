package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

/**
 * Load-side counterpart to {@link EvCodecValueANode}: SAME type identifier (port UID matches)
 * but NO codec and NO accessor — the saved codec-encoded value has nothing to decode through.
 * deserializeFailed should fire and the value falls back to the builder default.
 */
@NodeAttribute(name = "ev_no_codec", group = "test", graphTypes = {TestGraph.class})
public class EvNoCodecNode extends Node {
    @Override public Component getDisplayName() { return Component.literal("Ev No Codec"); }
    @Override public void onDefinePorts(IPortDefinitionContext context) {
        var b = context.addInputPort("port", TypeHandleHelpers.fromType(SchemaEvolutionTestNodes.EvCodecValueA.class));
        b.withDefaultValue(new SchemaEvolutionTestNodes.EvCodecValueA(55, "no-codec-default"));
        b.build();
        context.addOutputPort("out", Float.class).build();
    }
}
