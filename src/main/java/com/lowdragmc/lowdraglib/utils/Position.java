package com.lowdragmc.lowdraglib.utils;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector2f;

import java.util.List;
import java.util.Objects;

@Getter
public final class Position {
    public final static Codec<Position> CODEC = Codec.INT.listOf().comapFlatMap(
            list -> Util.fixedSize(list, 2).map(p_253489_ -> Position.of(list.get(0), list.get(1))),
            position -> List.of(position.x, position.y)
    );

    public final static StreamCodec<FriendlyByteBuf, Position> STREAM_CODEC = StreamCodec.of(
            (byteBuf, position) -> {
                byteBuf.writeVarInt(position.x);
                byteBuf.writeVarInt(position.y);
            },
            byteBuf -> new Position(byteBuf.readVarInt(), byteBuf.readVarInt())
    );

    public static final Position ORIGIN = new Position(0, 0);

    public final int x;
    public final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Position of(int x, int y) {
        return new Position(x, y);
    }

    public Position add(Position other) {
        return new Position(x + other.x, y + other.y);
    }

    public Position add(int x, int y) {
        return new Position(this.x + x, this.y + y);
    }

    public Position subtract(Position other) {
        return new Position(x - other.x, y - other.y);
    }

    public Position add(Size size) {
        return new Position(x + size.width, y + size.height);
    }

    public Position addX(int x) {
        return new Position(this.x + x,y);
    }

    public Position addY(int y){
        return new Position(x,this.y + y);
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
