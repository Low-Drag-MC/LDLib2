package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Shared records used by the schema-evolution test node family ({@code EvCodecValueANode},
 * {@code EvCodecValueBNode}, {@code EvNoCodecNode}, ...). The two record shapes intentionally
 * differ so codec A's encoding fails to parse through codec B — exercising the
 * "codec → different codec" deserialize-failure path.
 *
 * <p>Each schema-evolution test node is its own top-level public file so it can be instantiated
 * via {@code Class.getConstructor()} reflection in {@code GraphModel.findNodeByClassName}.</p>
 */
public final class SchemaEvolutionTestNodes {
    private SchemaEvolutionTestNodes() {}

    public record EvCodecValueA(int a, String b) {
        public static final Codec<EvCodecValueA> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("a").forGetter(EvCodecValueA::a),
                Codec.STRING.fieldOf("b").forGetter(EvCodecValueA::b)
        ).apply(i, EvCodecValueA::new));
    }

    /** Different shape — codec A's record-shaped NBT cannot parse through codec B. */
    public record EvCodecValueB(double x, java.util.List<Integer> ys) {
        public static final Codec<EvCodecValueB> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(EvCodecValueB::x),
                Codec.list(Codec.INT).fieldOf("ys").forGetter(EvCodecValueB::ys)
        ).apply(i, EvCodecValueB::new));
    }
}
