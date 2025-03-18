package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.accessor.IManagedObjectAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Objects;

public class IManagedReadOnlyRef extends ReadonlyRef<IManaged> {

    public IManagedReadOnlyRef(IManaged managed, ManagedKey key, IManagedObjectAccessor accessor) {
        super(managed, key, accessor);
    }

    public IManaged getManaged() {
        return Objects.requireNonNull(readRaw());
    }

    @Override
    public void clearSyncDirty() {
        for (var field : getManaged().getSyncStorage().getSyncFields()) {
            field.clearSyncDirty();
        }
        super.clearSyncDirty();
    }

    @Override
    public void clearPersistedDirty() {
        for (var field : getManaged().getSyncStorage().getPersistedFields()) {
            field.clearPersistedDirty();
        }
        super.clearPersistedDirty();
    }

    @Override
    public void update() {
        var storage = getManaged().getSyncStorage();

        for (IRef field : storage.getNonLazyFields()) {
            field.update();
        }

        if (storage.hasDirtySyncFields()) {
            if (getKey().isDestSync()) {
                markAsDirty();
            } else {
                for (var field : storage.getSyncFields()) {
                    field.clearSyncDirty();
                }
            }
        }

        if (storage.hasDirtyPersistedFields()) {
            if (getKey().isPersist()) {
                markAsDirty();
            } else {
                for (var field : storage.getPersistedFields()) {
                    field.clearPersistedDirty();
                }
            }
        }
    }

    @Override
    public <T> T readPersisted(DynamicOps<T> op) {
        var persistedFields = getManaged().getSyncStorage().getPersistedFields();
        var map = new HashMap<T, T>();
        for (var persistedField : persistedFields) {
            var key = persistedField.getPersistedKey();
            var data = persistedField.readPersisted(op);
            map.put(op.createString(key), data);
        }
        return op.createMap(map);
    }

    @Override
    public <T> void writePersisted(DynamicOps<T> op, T payload) {
        var persistedFields = getManaged().getSyncStorage().getPersistedFields();
        var map = op.getMap(payload).getOrThrow();
        for (var persistedField : persistedFields) {
            var key = persistedField.getPersistedKey();
            var data = map.get(op.createString(key));
            if (data != null) {
                persistedField.writePersisted(op, data);
            }
        }
    }

    @Override
    public void readSyncToStream(RegistryFriendlyByteBuf buffer) {
        var syncedFields = getManaged().getSyncStorage().getSyncFields();
        var changed = new BitSet();
        for (int i = 0; i < syncedFields.length; i++) {
            var syncedField = syncedFields[i];
            if (syncedField.isSyncDirty()) {
                changed.set(i);
            }
        }
        buffer.writeByteArray(changed.toByteArray());
        for (int i = 0; i < syncedFields.length; i++) {
            if (changed.get(i)) {
                var syncedField = syncedFields[i];
                syncedField.readSyncToStream(buffer);
                syncedField.clearSyncDirty();
            }
        }
    }

    @Override
    public void writeSyncFromStream(RegistryFriendlyByteBuf buffer) {
        var syncedFields = getManaged().getSyncStorage().getSyncFields();
        var changed = BitSet.valueOf(buffer.readByteArray());
        for (int i = 0; i < syncedFields.length; i++) {
            if (changed.get(i)) {
                var syncedField = syncedFields[i];
                syncedField.writeSyncFromStream(buffer);
            }
        }
    }
}
