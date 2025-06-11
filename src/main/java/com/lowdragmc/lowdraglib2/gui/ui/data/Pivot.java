package com.lowdragmc.lowdraglib2.gui.ui.data;

import com.mojang.serialization.Codec;
import lombok.Data;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

@Data
public final class Pivot {
    public final static Codec<Pivot> CODEC = Codec.FLOAT.listOf().comapFlatMap(
            list -> Util.fixedSize(list, 2).map(l -> Pivot.of(l.get(0), l.get(1))),
            pivot -> List.of(pivot.x, pivot.y)
    );

    public final static StreamCodec<FriendlyByteBuf, Pivot> STREAM_CODEC = StreamCodec.of(
            (byteBuf, pivot) -> {
                byteBuf.writeFloat(pivot.x);
                byteBuf.writeFloat(pivot.y);
            },
            byteBuf -> Pivot.of(byteBuf.readFloat(), byteBuf.readFloat())
    );
    public static final Pivot TOP_LEFT = of(0, 0);
    public static final Pivot TOP_CENTER = of(0.5f, 0);
    public static final Pivot TOP_RIGHT = of(1, 0);
    public static final Pivot CENTER_LEFT = of(0, 0.5f);
    public static final Pivot CENTER = of(0.5f, 0.5f);
    public static final Pivot CENTER_RIGHT = of(1, 0.5f);
    public static final Pivot BOTTOM_LEFT = of(0, 1);
    public static final Pivot BOTTOM_CENTER = of(0.5f, 1);
    public static final Pivot BOTTOM_RIGHT = of(1, 1);

    public static final Pivot[] VALUES = {
            TOP_LEFT, TOP_CENTER, TOP_RIGHT,
            CENTER_LEFT, CENTER, CENTER_RIGHT,
            BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    };

    public final float x;
    public final float y;

    private Pivot(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public static Pivot of(float x, float y) {
        return new Pivot(x, y);
    }

}
