package com.lowdragmc.lowdraglib2.math;

import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

/**
 * @author KilaBash
 * @date 2023/5/30
 * @implNote Range
 */
@Data(staticConstructor = "of")
public final class Range {

    public final static Codec<Range> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LDLibExtraCodecs.NUMBER.fieldOf("a").forGetter(range -> range.a),
            LDLibExtraCodecs.NUMBER.fieldOf("b").forGetter(range -> range.b)
    ).apply(instance, Range::of));

    public final static StreamCodec<FriendlyByteBuf, Range> STREAM_CODEC = StreamCodec.of(
            (byteBuf, range) -> {
                byteBuf.writeDoubleLE(range.a.doubleValue());
                byteBuf.writeDoubleLE(range.a.doubleValue());
            },
            byteBuf -> new Range(byteBuf.readDoubleLE(), byteBuf.readDoubleLE())
    );

    private final Number a, b;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Range range && range.a.equals(a) && range.b.equals(b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    public Number getMin() {
        return a.doubleValue() < b.doubleValue() ? a : b;
    }

    public Number getMax() {
        return a.doubleValue() > b.doubleValue() ? a : b;
    }

}
