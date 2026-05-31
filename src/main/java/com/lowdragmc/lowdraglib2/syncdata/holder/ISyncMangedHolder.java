package com.lowdragmc.lowdraglib2.syncdata.holder;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.async.AsyncThreadData;
import com.lowdragmc.lowdraglib2.async.IAsyncLogic;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import com.lowdragmc.lowdraglib2.utils.TagBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.BitSet;
import java.util.concurrent.RejectedExecutionException;


/**
 * A block entity that can be automatically synced with the client.
 *
 * @see DescSynced
 * @see LazyManaged
 */
public interface ISyncMangedHolder extends IManagedHolder, IAsyncLogic {

    CustomPacketPayload createSyncPacket(BitSet changed, byte[] data, CompoundTag extra);

    /**
     * do a sync now. if the block entity is tickable then this would be handled automatically, I think.
     *
     * @param force if true, all fields will be synced, otherwise only the ones that have changed will be synced
     */
    default void sync(boolean force) {
        var rootStorage = this.getRootStorage();
        for (var field : rootStorage.getNonLazyFields()) {
            field.update();
        }
        if (rootStorage.hasDirtySyncFields()) {
            var changed = new BitSet();
            var syncedFields = rootStorage.getSyncFields();
            var serverLevel = getServerLevel();
            var server = serverLevel.getServer();
            if (!Platform.serverSafe(server)) return;
            var data = ByteBufUtil.writeCustomData(buffer -> {
                for (int i = 0; i < syncedFields.length; i++) {
                    var field = syncedFields[i];
                    if (force || field.isSyncDirty()) {
                        changed.set(i);
                        field.readSyncToStream(buffer);
                        field.clearSyncDirty();
                    }
                }
            }, serverLevel.registryAccess());
            try {
                server.executeIfPossible(() -> {
                    if (!Platform.serverSafe(server)) return;
                    var extra = new CompoundTag();
                    writeCustomSyncData(serverLevel.registryAccess(), extra);
                    var packet = createSyncPacket(changed, data, extra);
                    PacketDistributor.sendToPlayersTrackingChunk(serverLevel, this.getTrackingPos(), packet);
                });
            } catch (RejectedExecutionException ignored) {
                // The server can begin shutting down between the safety check and task submission.
            }
        }
    }

    default void passivelySync() {
        sync(false);
    }

    /**
     * write custom data to the packet. it will always be synced.
     */
    default void writeCustomSyncData(HolderLookup.Provider provider, CompoundTag tag) {
    }

    /**
     * read custom data from the packet
     */
    default void readCustomSyncData(HolderLookup.Provider provider, CompoundTag tag) {
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
    default CompoundTag serializeInitialData(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        var customTag = new CompoundTag();
        writeCustomSyncData(provider, customTag);
        if (!customTag.isEmpty()) {
            tag.put("custom", customTag);
        }

        var list = new ListTag();
        var syncedFields = getRootStorage().getSyncFields();
        var ctx = provider.createSerializationContext(NbtOps.INSTANCE);
        for (IRef<?> syncedField : syncedFields) {
            list.add(TagBuilder.compound().add("d", syncedField.readInitialSync(ctx)).build());
        }
        if (!list.isEmpty()) {
            tag.put("managed", list);
        }
        return tag;
    }

    /**
     * This is called when the block entity is first created on the client, and deserialize initial data at client side.
     */
    default void deserializeInitialData(HolderLookup.Provider provider, CompoundTag tag) {
        var customTag = tag.getCompound("custom");
        readCustomSyncData(provider, customTag);

        var list = tag.getList("managed", Tag.TAG_COMPOUND);
        var syncedFields = getRootStorage().getSyncFields();
        if (syncedFields.length != list.size()) {
            throw new IllegalStateException("Synced fields count mismatch");
        }
        var ctx = provider.createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < list.size(); i++) {
            var data = list.getCompound(i).get("d");
            syncedFields[i].writeInitialSync(ctx, data);
        }
    }

    default void handleSyncPacket(RegistryAccess registryAccess, BitSet changed, byte[] data, CompoundTag extra) {
        ByteBufUtil.readCustomData(data, buffer -> {
            var storage = getRootStorage();
            var syncedFields = storage.getSyncFields();
            for (int i = 0; i < syncedFields.length; i++) {
                if (changed.get(i)) {
                    var field = syncedFields[i];
                    var key = field.getKey();
                    if (storage.hasSyncListener(key)) {
                        var postStream = storage.notifyFieldUpdate(key, field.readRaw());
                        field.writeSyncFromStream(buffer);
                        postStream.forEach(consumer -> consumer.accept(field.readRaw()));
                    } else {
                        field.writeSyncFromStream(buffer);
                    }
                }
            }
        }, registryAccess);
        readCustomSyncData(registryAccess, extra);
    }

    /// Async
    default boolean isAsyncValid() {
        return true;
    }

    default boolean useAsyncThread() {
        return false;
    }

    default void attachAsyncLogic() {
        if (useAsyncThread()) {
            AsyncThreadData.getOrCreate(getServerLevel()).addAsyncLogic(this);
        }
    }

    default void detachAsyncLogic() {
        if (useAsyncThread()) {
            AsyncThreadData.getOrCreate(getServerLevel()).removeAsyncLogic(this);
        }
    }

    @Override
    default void asyncTick(long periodID) {
        if (Platform.isServerNotSafe()) return;

        if (useAsyncThread() && isAsyncValid()) {
            passivelySync();
        }
    }
}
