package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IManagedObjectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.var.ReadOnlyVar;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.*;

public class IManagedReadOnlyRef extends ReadOnlyRef<IManaged> {

    public IManagedReadOnlyRef(ReadOnlyVar<IManaged> managed, ManagedKey key, IManagedObjectAccessor accessor) {
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

        for (IRef<?> field : storage.getNonLazyFields()) {
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
        for (IRef<?> persistedField : persistedFields) {
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
        for (IRef<?> persistedField : persistedFields) {
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
        for (IRef<?> syncedField : syncedFields) {
            list.add(syncedField.readInitialSync(op));
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
            IRef<?> syncedField = syncedFields[i];
            var data = list.get(i);
            syncedField.writeInitialSync(op, data);
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
        var storage = getManaged().getSyncStorage();
        var syncedFields = storage.getSyncFields();
        var changed = BitSet.valueOf(buffer.readByteArray());
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
    }
}
