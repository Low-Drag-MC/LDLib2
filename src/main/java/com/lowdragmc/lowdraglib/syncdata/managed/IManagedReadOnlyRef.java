package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.accessor.IManagedObjectAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.*;

public class IManagedReadOnlyRef extends ReadonlyRef<IManaged> {

    public IManagedReadOnlyRef(ReadOnlyDirectField<IManaged> managed, ManagedKey key, IManagedObjectAccessor accessor) {
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
    public void readOnlyUpdate() {
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
    public <T> T readReadOnlyPersisted(DynamicOps<T> op) {
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
    public <T> void writeReadOnlyPersisted(DynamicOps<T> op, T payload) {
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
    public <T> T readReadOnlySync(DynamicOps<T> op) {
        var syncedFields = getManaged().getSyncStorage().getSyncFields();
        var list = new ArrayList<T>();
        for (var syncedField : syncedFields) {
            list.add(syncedField.readSync(op));
        }
        return op.createList(list.stream());
    }

    @Override
    public <T> void writeReadOnlySync(DynamicOps<T> op, T payload) {
        var syncedFields = getManaged().getSyncStorage().getSyncFields();
        var list = op.getStream(payload).getOrThrow().toList();
        if (list.size() != syncedFields.length) {
            throw new IllegalArgumentException("Size of list does not match size of synced fields");
        }
        for (int i = 0; i < syncedFields.length; i++) {
            var syncedField = syncedFields[i];
            var data = list.get(i);
            syncedField.writeSync(op, data);
        }
    }

    @Override
    public void readReadOnlySyncToStream(RegistryFriendlyByteBuf buffer) {
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
            }
        }
    }

    @Override
    public void writeReadOnlySyncFromStream(RegistryFriendlyByteBuf buffer) {
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
