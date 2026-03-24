package com.lowdragmc.lowdraglib2.gui.ui.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Immutable 2D translation with per-axis px/percent support.
 */
@Data
public final class Translate2D {
    public static final Translate2D ZERO = new Translate2D(LengthPercent.ZERO, LengthPercent.ZERO);

    public static final Codec<Translate2D> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LengthPercent.CODEC.fieldOf("x").forGetter(Translate2D::getX),
            LengthPercent.CODEC.fieldOf("y").forGetter(Translate2D::getY)
    ).apply(instance, Translate2D::new));

    public static final StreamCodec<FriendlyByteBuf, Translate2D> STREAM_CODEC = StreamCodec.of(
            (buf, t) -> {
                LengthPercent.STREAM_CODEC.encode(buf, t.x);
                LengthPercent.STREAM_CODEC.encode(buf, t.y);
            },
            buf -> new Translate2D(
                    LengthPercent.STREAM_CODEC.decode(buf),
                    LengthPercent.STREAM_CODEC.decode(buf)
            )
    );

    private final LengthPercent x;
    private final LengthPercent y;

    public Translate2D(LengthPercent x, LengthPercent y) {
        this.x = x;
        this.y = y;
    }

    public static Translate2D px(float x, float y) {
        return new Translate2D(LengthPercent.px(x), LengthPercent.px(y));
    }

    public static Translate2D percent(float x, float y) {
        return new Translate2D(LengthPercent.percent(x), LengthPercent.percent(y));
    }

    public float resolveX(float width) {
        return x.resolve(width);
    }

    public float resolveY(float height) {
        return y.resolve(height);
    }

    public boolean isZero() {
        return x.isZero() && y.isZero();
    }

    /**
     * Returns true when both axes are pure pixel values (no percent).
     */
    public boolean isPx() {
        return !x.isPercent() && !y.isPercent();
    }

    public static Translate2D lerp(Translate2D a, Translate2D b, float t) {
        return new Translate2D(
                LengthPercent.lerp(a.x, b.x, t),
                LengthPercent.lerp(a.y, b.y, t)
        );
    }
}
