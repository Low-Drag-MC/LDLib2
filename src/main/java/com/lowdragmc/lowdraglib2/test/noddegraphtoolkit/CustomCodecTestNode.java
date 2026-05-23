package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

/**
 * Exercises the per-port serialization / configurator opt-ins added alongside this node:
 *
 * <ul>
 *   <li>{@code codec_port} — {@link CodecValue} has no registered accessor; serialization
 *       round-trips entirely through the supplied {@link Codec}.</li>
 *   <li>{@code no_serialize_port} — Float port whose builder calls {@code withoutSerialization()};
 *       value should reset to default after a save/load.</li>
 *   <li>{@code no_config_port} — Float port whose builder calls {@code withoutConfigurator()};
 *       PortModel.isConfiguratorEnabled() should report false.</li>
 *   <li>{@code missing_port} — {@link CodecValue} port with no codec and no accessor; serialize
 *       must complete without throwing (graceful skip via the warn-once path).</li>
 * </ul>
 */
@NodeAttribute(name = "custom_codec_test", group = "test", graphTypes = {TestGraph.class})
public class CustomCodecTestNode extends Node {

    public record CodecValue(int a, String b) {
        public static final Codec<CodecValue> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("a").forGetter(CodecValue::a),
                Codec.STRING.fieldOf("b").forGetter(CodecValue::b)
        ).apply(i, CodecValue::new));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Custom Codec Test");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        // withDefaultValue returns null on PortBuilder (latent quirk), so it must be the LAST
        // call in any chain. Pull builders into locals to set every flag we need.
        var codecBuilder = context.addInputPort("codec_port", TypeHandleHelpers.fromType(CodecValue.class));
        codecBuilder.withCodec(CodecValue.CODEC);
        codecBuilder.withDefaultValue(new CodecValue(0, "default"));
        codecBuilder.build();

        var noSerBuilder = context.addInputPort("no_serialize_port", Float.class);
        noSerBuilder.withoutSerialization();
        noSerBuilder.withDefaultValue(1.0f);
        noSerBuilder.build();

        var noConfBuilder = context.addInputPort("no_config_port", Float.class);
        noConfBuilder.withoutConfigurator();
        noConfBuilder.withDefaultValue(2.0f);
        noConfBuilder.build();

        // No codec, no accessor — should serialize gracefully (no value emitted, no crash).
        context.addInputPort("missing_port", TypeHandleHelpers.fromType(CodecValue.class)).build();

        context.addOutputPort("out", Float.class).build();
    }
}
