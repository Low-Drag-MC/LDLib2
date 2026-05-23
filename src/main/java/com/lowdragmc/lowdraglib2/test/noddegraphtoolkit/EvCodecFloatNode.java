package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

/** Load-side counterpart to {@link EvAccessorFloatNode}: same Float port now serialized via Codec.FLOAT. */
@NodeAttribute(name = "ev_codec_float", group = "test", graphTypes = {TestGraph.class})
public class EvCodecFloatNode extends Node {
    @Override public Component getDisplayName() { return Component.literal("Ev Codec Float"); }
    @Override public void onDefinePorts(IPortDefinitionContext context) {
        var b = context.addInputPort("port", Float.class);
        b.withCodec(Codec.FLOAT);
        b.withDefaultValue(7.0f);
        b.build();
        context.addOutputPort("out", Float.class).build();
    }
}
