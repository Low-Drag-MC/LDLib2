package com.lowdragmc.lowdraglib.math;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import lombok.Data;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector2f;

import java.util.List;
import java.util.Objects;

@Data(staticConstructor = "of")
public final class Position {
    public final static Codec<Position> CODEC = Codec.INT.listOf().comapFlatMap(
            list -> Util.fixedSize(list, 2).map(l -> Position.of(l.get(0), l.get(1))),
            position -> List.of(position.x, position.y)
    );

    public final static StreamCodec<FriendlyByteBuf, Position> STREAM_CODEC = StreamCodec.of(
            (byteBuf, position) -> {
                byteBuf.writeVarInt(position.x);
                byteBuf.writeVarInt(position.y);
            },
            byteBuf -> Position.of(byteBuf.readVarInt(), byteBuf.readVarInt())
    );

    public static final Position ORIGIN = Position.of(0, 0);

    public final int x;
    public final int y;

    public Position add(Position other) {
        return Position.of(x + other.x, y + other.y);
    }

    public Position add(int x, int y) {
        return Position.of(this.x + x, this.y + y);
    }

    public Position subtract(Position other) {
        return Position.of(x - other.x, y - other.y);
    }

    public Position add(Size size) {
        return Position.of(x + size.width, y + size.height);
    }

    public Position addX(int x) {
        return Position.of(this.x + x,y);
    }

    public Position addY(int y){
        return Position.of(x,this.y + y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position position)) return false;
        return x == position.x &&
                y == position.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("x", x)
                .add("y", y)
                .toString();
    }

    public Vector2f vector2f() {
        return new Vector2f(x, y);
    }

    public Vec2 vec2() {
        return new Vec2(x, y);
    }
}
