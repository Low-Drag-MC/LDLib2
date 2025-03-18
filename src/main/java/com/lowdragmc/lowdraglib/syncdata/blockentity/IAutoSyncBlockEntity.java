package com.lowdragmc.lowdraglib.syncdata.blockentity;

import com.lowdragmc.lowdraglib.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;

/**
 * A block entity that can be automatically synced with the client.
 *
 * @see DescSynced
 * @see LazyManaged
 */
public interface IAutoSyncBlockEntity extends IManagedBlockEntity {

    /**
     * do a sync now. if the block entity is tickable then this would be handled automatically, I think.
     *
     * @param force if true, all fields will be synced, otherwise only the ones that have changed will be synced
     */
    default void syncNow(boolean force) {
        var level = Objects.requireNonNull(getSelf().getLevel());
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Cannot sync from client");
        }
        for (IRef field : this.getNonLazyFields()) {
            field.update();
        }
        var packet = SPacketAutoSyncBlockEntity.of(this, force);
        PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(this.getCurrentPos()), packet);
    }


    default void defaultServerTick() {
        if (!(getSelf().getLevel() instanceof ServerLevel serverLevel)) return;
        for (IRef field : getNonLazyFields()) {
            field.update();
        }
        if (getRootStorage().hasDirtySyncFields()) {
            var packet = SPacketAutoSyncBlockEntity.of(this, false);
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(this.getCurrentPos()), packet);
        }
    }

    /**
     * write custom data to the packet
     */
    default void writeCustomSyncData(CompoundTag tag) {
    }

    /**
     * read custom data from the packet
     */
    default void readCustomSyncData(CompoundTag tag) {
    }


    /**
     * sync tag name
     */
    default String getSyncTag() {
        return "sync";
    }
}
