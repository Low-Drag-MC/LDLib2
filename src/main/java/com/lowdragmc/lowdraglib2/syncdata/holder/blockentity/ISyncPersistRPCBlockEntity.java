package com.lowdragmc.lowdraglib2.syncdata.holder.blockentity;

import com.lowdragmc.lowdraglib2.syncdata.holder.IPersistManagedHolder;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface ISyncPersistRPCBlockEntity extends ISyncBlockEntity, IRPCBlockEntity, IPersistManagedHolder, IBlockEntityManaged {
    @Override
    default IManagedStorage getRootStorage() {
        return getSyncStorage();
    }

    @Override
    default boolean isAsyncValid() {
        return !getSelf().isRemoved();
    }

    @Override
    default boolean useAsyncThread() {
        return true;
    }

    @Environment(EnvType.CLIENT)
    default void rpcToServer(String methodName, Object... args) {
        rpcToServer(this, methodName, args);
    }

    default void rpcToPlayer(ServerPlayer player, String methodName, Object... args) {
        rpcToPlayer(this, player, methodName, args);
    }

    default void rpcToTracking(String methodName, Object... args) {
        rpcToTracking(this, methodName, args);
    }
}
