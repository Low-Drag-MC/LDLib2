package com.lowdragmc.lowdraglib2.client.model.custommodel;

import com.lowdragmc.lowdraglib2.utils.ShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2023/3/23
 * @implNote ConnectInfo
 */
public enum Connection {
    UP(Direction.UP),
    DOWN(Direction.DOWN),
    LEFT(Direction.EAST),
    RIGHT(Direction.WEST),
    UP_LEFT(Direction.UP, Direction.EAST),
    UP_RIGHT(Direction.UP, Direction.WEST),
    DOWN_LEFT(Direction.DOWN, Direction.EAST),
    DOWN_RIGHT(Direction.DOWN, Direction.WEST);

    public final Direction[] dirs;
    public final BlockPos[] offsets;

    Connection(Direction... dirs) {
        this.dirs = dirs;
        this.offsets = new BlockPos[6];
        for (var normal : Direction.values()) {
            var pos = BlockPos.ZERO;
            for (Direction dir : dirs) {
                if (normal.getAxis() == Direction.Axis.Y) {
                    dir = dir.getOpposite();
                }
                pos = pos.relative(dir);
            }
            var rotated = ShapeUtils.rotate(new AABB(pos), normal);
            offsets[normal.ordinal()] = new BlockPos(Mth.floor((rotated.minX + rotated.maxX) / 2), Mth.floor((rotated.minY + rotated.maxY) / 2), Mth.floor((rotated.minZ + rotated.maxZ) / 2));
        }
    }

    @Nonnull
    public BlockPos getOffset(Direction normal) {
        return this.offsets[normal.ordinal()];
    }

    @Nonnull
    public BlockPos transform(BlockPos pos, Direction normal) {
        return pos.offset(getOffset(normal));
    }

    public Connection getOpposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case UP_LEFT -> DOWN_RIGHT;
            case UP_RIGHT -> DOWN_LEFT;
            case DOWN_LEFT -> UP_RIGHT;
            case DOWN_RIGHT -> UP_LEFT;
        };
    }

}
