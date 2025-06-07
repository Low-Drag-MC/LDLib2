package com.lowdragmc.lowdraglib2.math;

import com.google.common.base.MoreObjects;
import lombok.Data;

import java.util.Objects;

@Data(staticConstructor = "of")
public final class PositionedRect {
    public final Position position;
    public final Size size;

    public static PositionedRect of(int x, int y, int width, int height) {
        return of(Position.of(x, y), Size.of(width, height));
    }

    public static PositionedRect of(Position pos1, Position pos2) {
        var position = Position.of(Math.min(pos1.x, pos2.x), Math.min(pos1.y, pos2.y));
        var size = Size.of(Math.max(pos1.x, pos2.x) - position.x, Math.max(pos1.y, pos2.y) - position.y);
        return PositionedRect.of(position, size);
    }

    public boolean intersects(Position other) {
        return position.x <= other.x &&
                position.y <= other.y &&
                position.x + size.width >= other.x &&
                position.y + size.height >= other.y;
    }

    public boolean intersects(PositionedRect other) {
        return intersects(other.position) ||
                intersects(other.position.add(other.size)) ||
                intersects(other.position.add(Size.of(other.size.width, 0))) ||
                intersects(other.position.add(Size.of(0, other.size.height)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionedRect)) return false;
        PositionedRect that = (PositionedRect) o;
        return position.equals(that.position) &&
                size.equals(that.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, size);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("position", position)
                .add("size", size)
                .toString();
    }
}
