package com.lowdragmc.lowdraglib2.client.model.custommodel;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author KilaBash
 * @date 2023/3/24
 * @implNote Connections
 */
public class Connections {

    /** Some hardcoded offset values for the different corner indeces */
    protected static int[] submapOffsets = { 4, 5, 1, 0 };

    // Mapping the different corner indeces to their respective dirs
    protected static final Connection[][] submapMap = new Connection[][] {
            { Connection.DOWN, Connection.LEFT, Connection.DOWN_LEFT },
            { Connection.DOWN, Connection.RIGHT, Connection.DOWN_RIGHT },
            { Connection.UP, Connection.RIGHT, Connection.UP_RIGHT },
            { Connection.UP, Connection.LEFT, Connection.UP_LEFT }
    };

    @Getter
    private int[] submapIndices = new int[] { 18, 19, 17, 16 };

    private byte connections = 0;

    private Connections(Connection... connections) {
        for (Connection connection : connections) {
            add(connection);
        }
    }

    public static Connections of(Connection... connections) {
       return new Connections(connections);
    }

    public boolean contains(Connection connection) {
        return (connections & (1 << (connection.ordinal()))) > 0;
    }

    public void add(Connection connection) {
        this.connections |= 1 << (connection.ordinal());
    }

    public boolean isEmpty() {
        return connections == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Connections that = (Connections) o;

        return connections == that.connections;
    }

    @Override
    public int hashCode() {
        return connections;
    }

    public static Connections checkConnections(BlockAndTintGetter level, BlockPos pos, @Nonnull BlockState state, @Nullable Direction side) {
        Connections connections = Connections.of();
        if (side != null) {
            pos = pos.immutable();
            for (var connection : Connection.values()) {
                var offset = connection.transform(pos, side);
                var adjacent = level.getBlockState(offset);
                if (ICTMPredicate.getPredicate(state).isConnected(level, state, pos, adjacent, offset, side)) {
                    connections.add(connection);
                }
            }
        }
        // Map connections to submap indeces
        for (int i = 0; i < 4; i++) {
            connections.fillSubmaps(i);
        }
        return connections;
    }

    /**
     * @param dirs
     *            The directions to check connection in.
     * @return True if the cached connectionMap holds a connection in <i><b>all</b></i> the given {@link Connection directions}.
     */
    @SuppressWarnings("null")
    public boolean connectedAnd(Connection... dirs) {
        for (Connection dir : dirs) {
            if (!contains(dir)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param dirs
     *            The directions to check connection in.
     * @return True if the cached connectionMap holds a connection in <i><b>one of</b></i> the given {@link Connection directions}.
     */
    @SuppressWarnings("null")
    public boolean connectedOr(Connection... dirs) {
        for (Connection dir : dirs) {
            if (contains(dir)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("null")
    protected void fillSubmaps(int idx) {
        Connection[] dirs = submapMap[idx];
        if (connectedOr(dirs[0], dirs[1])) {
            if (connectedAnd(dirs)) {
                // If all dirs are connected, we use the fully connected face,
                // the base offset value.
                submapIndices[idx] = submapOffsets[idx];
            } else {
                // This is a bit magic-y, but basically the array is ordered so
                // the first dir requires an offset of 2, and the second dir
                // requires an offset of 8, plus the initial offset for the
                // corner.
                submapIndices[idx] = submapOffsets[idx] + (contains(dirs[0]) ? 2 : 0) + (contains(dirs[1]) ? 8 : 0);
            }
        }
    }
}
