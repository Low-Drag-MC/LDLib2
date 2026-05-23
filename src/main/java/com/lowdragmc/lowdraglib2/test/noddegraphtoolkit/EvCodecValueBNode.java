package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

/**
 * Load-side counterpart to {@link EvCodecValueANode}: SAME type identifier (so the port UID
 * matches across the swap) but a DIFFERENT codec body that expects a string-shaped NBT, not
 * the record-shaped NBT codec A emits. Loading codec A's output through this codec must fail.
 */
@NodeAttribute(name = "ev_codec_value_b", group = "test", graphTypes = {TestGraph.class})
public class EvCodecValueBNode extends Node {
    @Override public Component getDisplayName() { return Component.literal("Ev Codec Value B"); }
    @Override public void onDefinePorts(IPortDefinitionContext context) {
        var b = context.addInputPort("port", TypeHandleHelpers.fromType(SchemaEvolutionTestNodes.EvCodecValueA.class));
        b.withCodec(transcodingCodec());
        b.withDefaultValue(new SchemaEvolutionTestNodes.EvCodecValueA(123, "default-B"));
        b.build();
        context.addOutputPort("out", Float.class).build();
    }

    private static Codec<SchemaEvolutionTestNodes.EvCodecValueA> transcodingCodec() {
        return Codec.STRING.xmap(
                s -> new SchemaEvolutionTestNodes.EvCodecValueA(0, s),
                SchemaEvolutionTestNodes.EvCodecValueA::b
        );
    }
}
