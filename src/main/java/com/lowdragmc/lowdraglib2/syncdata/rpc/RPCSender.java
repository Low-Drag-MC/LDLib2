package com.lowdragmc.lowdraglib2.syncdata.rpc;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the sender of an RPC call.

 */
public interface RPCSender {
    /**
     * @return true if the sender is the server, false if the sender is a client.
     */
    boolean isServer();

    /**
     * If the rpc was sent by a remote player, this method will return the player that sent the rpc.
     */
    @Nullable
    default ServerPlayer asPlayer() {
        return null;
    }

    record ClientRPCSender(ServerPlayer player) implements RPCSender {
        @Override
        public boolean isServer() {
            return false;
        }

        @Override
        public @Nullable ServerPlayer asPlayer() {
            return player;
        }
    }

    static RPCSender ofClient(ServerPlayer player) {
        return new ClientRPCSender(player);
    }

    static RPCSender ofServer() {
        return () -> true;
    }


}
