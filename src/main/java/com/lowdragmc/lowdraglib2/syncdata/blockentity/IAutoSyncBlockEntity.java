package com.lowdragmc.lowdraglib2.syncdata.blockentity;

import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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
     * write custom data to the packet. it will always be synced.
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

    /**
     * This is called when the block entity is first created on the client, and prepare initial data at the server side.
     */
    default CompoundTag serializeInitialData() {
        var tag = new CompoundTag();
        var customTag = new CompoundTag();
        writeCustomSyncData(customTag);
        if (!customTag.isEmpty()) {
            tag.put("custom", customTag);
        }

        var list = new ListTag();
        var syncedFields = getRootStorage().getSyncFields();
        for (IRef<?> syncedField : syncedFields) {
            list.add(syncedField.readInitialSync(NbtOps.INSTANCE));
        }
        if (!list.isEmpty()) {
            tag.put("managed", list);
        }
        return tag;
    }

    /**
     * This is called when the block entity is first created on the client, and deserialize initial data at client side.
     */
    default void deserializeInitialData(CompoundTag tag) {
        var customTag = tag.getCompound("custom");
        readCustomSyncData(customTag);

        var list = tag.getList("managed", Tag.TAG_COMPOUND);
        var syncedFields = getRootStorage().getSyncFields();
        if (syncedFields.length != list.size()) {
            throw new IllegalStateException("Synced fields count mismatch");
        }
        for (int i = 0; i < list.size(); i++) {
            syncedFields[i].writeInitialSync(NbtOps.INSTANCE, list.getCompound(i));
        }
    }
}
