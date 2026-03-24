package com.lowdragmc.lowdraglib2.gui.ui.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * A length value that can be either an absolute pixel value or a percentage.
 * Percentage uses CSS convention: 50 means 50%.
 */
@Data
public final class LengthPercent {
    public static final LengthPercent ZERO = new LengthPercent(0f, false);

    public static final Codec<LengthPercent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("value").forGetter(LengthPercent::getValue),
            Codec.BOOL.fieldOf("percent").forGetter(LengthPercent::isPercent)
    ).apply(instance, LengthPercent::new));

    public static final StreamCodec<FriendlyByteBuf, LengthPercent> STREAM_CODEC = StreamCodec.of(
            (buf, lp) -> {
                buf.writeFloat(lp.value);
                buf.writeBoolean(lp.percent);
            },
            buf -> new LengthPercent(buf.readFloat(), buf.readBoolean())
    );

    private final float value;
    private final boolean percent;

    public LengthPercent(float value, boolean percent) {
        this.value = value;
        this.percent = percent;
    }

    public static LengthPercent px(float value) {
        return new LengthPercent(value, false);
    }

    public static LengthPercent percent(float value) {
        return new LengthPercent(value, true);
    }

    /**
     * Resolve this length against a dimension.
     * For percent, returns (value / 100) * dimension.
     * For px, returns value as-is.
     */
    public float resolve(float dimension) {
        return percent ? (value / 100f) * dimension : value;
    }

    public boolean isZero() {
        return value == 0f;
    }

    /**
     * Interpolate between two LengthPercent values.
     * Smooth lerp for same-type, snap at t=0.5 for mixed types.
     */
    public static LengthPercent lerp(LengthPercent a, LengthPercent b, float t) {
        if (a.percent == b.percent) {
            return new LengthPercent(a.value + (b.value - a.value) * t, a.percent);
        }
        // Mixed types: snap at 0.5
        return t < 0.5f ? a : b;
    }
}
