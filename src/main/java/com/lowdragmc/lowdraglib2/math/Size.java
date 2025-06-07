package com.lowdragmc.lowdraglib2.math;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import lombok.Data;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Objects;

@Data(staticConstructor = "of")
public final class Size {
    public final static Codec<Size> CODEC = Codec.INT.listOf().comapFlatMap(
            list -> Util.fixedSize(list, 2).map(l -> Size.of(l.get(0), l.get(1))),
            size -> List.of(size.width, size.height)
    );

    public final static StreamCodec<FriendlyByteBuf, Size> STREAM_CODEC = StreamCodec.of(
            (byteBuf, size) -> {
                byteBuf.writeVarInt(size.width);
                byteBuf.writeVarInt(size.height);
            },
            byteBuf -> Size.of(byteBuf.readVarInt(), byteBuf.readVarInt())
    );

    public static final Size ZERO = Size.of(0, 0);

    public final int width;
    public final int height;

    public static Size add(Position position) {
        return Size.of(position.x, position.y);
    }

    public Size add(Size other) {
        return Size.of(width + other.width, height + other.height);
    }

    public Size add(int width, int height) {
        return Size.of(this.width + width, this.height + height);
    }

    public Size subtract(Size other) {
        return Size.of(width - other.width, height - other.height);
    }

    public Size addWidth(int width) {
        return Size.of(this.width + width, height);
    }

    public Size addHeight(int height) {
        return Size.of(width, this.height + height);
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
