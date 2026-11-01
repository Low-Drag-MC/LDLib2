package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.misc.FluidStorage;
import com.lowdragmc.lowdraglib2.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib2.syncdata.IContentVersion;
import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.IPersistManagedHolder;
import com.lowdragmc.lowdraglib2.syncdata.holder.ISyncMangedHolder;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.stream.Stream;

/**
 * A managed holder that is not a block entity: {@link #sync} runs to completion on the calling thread and the packet
 * it builds is captured instead of sent, so tests and benchmarks can drive syncing one period at a time.
 */
public abstract class SyncProbeHolder implements IManaged, ISyncMangedHolder, IPersistManagedHolder {
    // sync() hands the captured packet to whoever tracks this chunk, and nobody can
    private static final ChunkPos BEYOND_WORLD_BORDER = new ChunkPos(2_000_000, 2_000_000);

    private final FieldManagedStorage storage = new FieldManagedStorage(this);
    private final ServerLevel level;
    @Nullable
    public BitSet sentChanged;
    @Nullable
    public byte[] sentData;
    public long packets;
    public long payloadBytes;
    public long persistenceNotices;

    protected SyncProbeHolder(ServerLevel level) {
        this.level = level;
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return storage;
    }

    @Override
    public IManagedStorage getRootStorage() {
        return storage;
    }

    @Override
    public ServerLevel getServerLevel() {
        return level;
    }

    @Override
    public ChunkPos getTrackingPos() {
        return BEYOND_WORLD_BORDER;
    }

    @Override
    public void notifyPersistence() {
        persistenceNotices++;
    }

    @Override
    public CustomPacketPayload createSyncPacket(BitSet changed, byte[] data, CompoundTag extra) {
        sentChanged = changed;
        sentData = data;
        packets++;
        payloadBytes += data.length + changed.toByteArray().length;
        return CapturedSync.INSTANCE;
    }

    /**
     * Initialises the storage and clears the dirty marks every field starts with, so the next sync only carries
     * real changes.
     */
    public void settle() {
        for (var ref : storage.getSyncFields()) {
            ref.clearSyncDirty();
        }
        for (var ref : storage.getPersistedFields()) {
            ref.clearPersistedDirty();
        }
    }

    /**
     * @return whether one sync period produced a packet, which {@code client} has then received
     */
    public boolean syncTo(@Nullable SyncProbeHolder client) {
        sentChanged = null;
        sentData = null;
        sync(false);
        if (sentData == null) return false;
        if (client != null) {
            client.handleSyncPacket(level.registryAccess(), sentChanged, sentData, new CompoundTag());
        }
        return true;
    }

    public void initClient(SyncProbeHolder client) {
        var access = level.registryAccess();
        client.deserializeInitialData(access, serializeInitialData(access));
    }

    public IRef<?> ref(String name) {
        return Stream.concat(Stream.of(storage.getSyncFields()), Stream.of(storage.getPersistedFields()))
                .filter(ref -> ref.getKey().getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no managed field " + name));
    }

    public int syncIndex(String name) {
        var fields = storage.getSyncFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getKey().getName().equals(name)) return i;
        }
        return -1;
    }

    public String syncFieldNames(@Nullable BitSet changed) {
        if (changed == null) return "[]";
        var fields = storage.getSyncFields();
        return changed.stream().mapToObj(i -> fields[i].getKey().getName()).toList().toString();
    }

    public Tag syncedValue(String name) {
        return ref(name).readInitialSync(level.registryAccess().createSerializationContext(NbtOps.INSTANCE));
    }

    /**
     * A nested managed object, the way a machine owns its recipe logic. It reports persistence to its owner.
     */
    public static class Part implements IManaged {
        private final FieldManagedStorage storage = new FieldManagedStorage(this);
        private final Runnable onPersist;

        @Persisted
        @DescSynced
        public int progress;
        @Persisted
        @DescSynced
        public int duration = 200;
        @Persisted
        @DescSynced
        public boolean working;
        @Persisted
        @DescSynced
        public long consumed;
        @Persisted
        @DescSynced
        public int parallel = 1;
        @Persisted
        @DescSynced
        public float speed = 1;
        @Persisted
        @DescSynced
        public boolean waiting;
        @Persisted
        @DescSynced
        public int tier;

        public Part(Runnable onPersist) {
            this.onPersist = onPersist;
        }

        @Override
        public IManagedStorage getSyncStorage() {
            return storage;
        }

        @Override
        public void notifyPersistence() {
            onPersist.run();
        }
    }

    /**
     * A container that saves state of its own next to its slots.
     */
    public static class LockableItems extends ItemStackTransfer {
        public boolean locked;

        public LockableItems(int size) {
            super(size);
        }

        @Override
        public void serialize(ValueOutput output) {
            super.serialize(output);
            output.putBoolean("locked", locked);
        }

        @Override
        public void deserialize(ValueInput input) {
            super.deserialize(input);
            locked = input.getBooleanOr("locked", false);
        }
    }

    public static class LockableTank extends FluidStorage {
        public boolean locked;

        public LockableTank(int capacity) {
            super(capacity);
        }

        @Override
        public void serialize(ValueOutput output) {
            super.serialize(output);
            output.putBoolean("locked", locked);
        }

        @Override
        public void deserialize(ValueInput input) {
            super.deserialize(input);
            locked = input.getBooleanOr("locked", false);
        }
    }

    /**
     * Saves extra state like {@link LockableItems}, so only its version tells the sync cheaply that it changed. The
     * version follows the handler's own change notifications and {@link #setLocked}, not stacks changed in place.
     */
    public static class VersionedItems extends LockableItems implements IContentVersion {
        private volatile long version;

        public VersionedItems(int size) {
            super(size);
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
            version++;
        }

        @Override
        protected void onContentsChanged(int slot) {
            version++;
            super.onContentsChanged(slot);
        }

        @Override
        public long getContentVersion() {
            return version;
        }
    }

    public static class VersionedTank extends LockableTank implements IContentVersion {
        private volatile long version;

        public VersionedTank(int capacity) {
            super(capacity);
        }

        @Override
        public void onContentsChanged() {
            version++;
            super.onContentsChanged();
        }

        @Override
        public long getContentVersion() {
            return version;
        }
    }

    private record CapturedSync() implements CustomPacketPayload {
        private static final CapturedSync INSTANCE = new CapturedSync();
        private static final Type<CapturedSync> TYPE = new Type<>(LDLib2.id("sync_probe"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
