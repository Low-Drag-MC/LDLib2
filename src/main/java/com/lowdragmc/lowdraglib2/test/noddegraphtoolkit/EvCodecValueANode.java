package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

/** Save-side schema: codec A on a non-accessor record type. */
@NodeAttribute(name = "ev_codec_value_a", group = "test", graphTypes = {TestGraph.class})
public class EvCodecValueANode extends Node {
    @Override public Component getDisplayName() { return Component.literal("Ev Codec Value A"); }
    @Override public void onDefinePorts(IPortDefinitionContext context) {
        var b = context.addInputPort("port", TypeHandleHelpers.fromType(SchemaEvolutionTestNodes.EvCodecValueA.class));
        b.withCodec(SchemaEvolutionTestNodes.EvCodecValueA.CODEC);
        b.withDefaultValue(new SchemaEvolutionTestNodes.EvCodecValueA(99, "default-A"));
        b.build();
        context.addOutputPort("out", Float.class).build();
    }
}
