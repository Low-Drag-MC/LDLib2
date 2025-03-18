package com.lowdragmc.lowdraglib.utils;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Objects;

@Getter
public class Size {

    public final static Codec<Size> CODEC = Codec.INT.listOf().comapFlatMap(
            list -> Util.fixedSize(list, 2).map(p_253489_ -> Size.of(list.get(0), list.get(1))),
            size -> List.of(size.width, size.height)
    );

    public final static StreamCodec<FriendlyByteBuf, Size> STREAM_CODEC = StreamCodec.of(
            (byteBuf, size) -> {
                byteBuf.writeVarInt(size.width);
                byteBuf.writeVarInt(size.height);
            },
            byteBuf -> new Size(byteBuf.readVarInt(), byteBuf.readVarInt())
    );

    public static final Size ZERO = new Size(0, 0);

    public final int width;
    public final int height;

    public Size(int width, int height) {
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public static Size of(int width, int height) {
        return new Size(width, height);
    }

    public static Size add(Position position) {
        return new Size(position.x, position.y);
    }

    public Size add(Size other) {
        return new Size(width + other.width, height + other.height);
    }

    public Size add(int width, int height) {
        return new Size(this.width + width, this.height + height);
    }

    public Size subtract(Size other) {
        return new Size(width - other.width, height - other.height);
    }

    public Size addWidth(int width) {
        return new Size(this.width + width, height);
    }

    public Size addHeight(int height) {
        return new Size(width, this.height + height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Size size)) return false;
        return width == size.width &&
                height == size.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("width", width)
                .add("height", height)
                .toString();
    }
}
