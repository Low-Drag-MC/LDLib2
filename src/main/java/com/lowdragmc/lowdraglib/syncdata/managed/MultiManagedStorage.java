package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.IFieldUpdateListener;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.ref.IRef;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author KilaBash
 * @date 2023/2/17
 * @implNote MultiManagedStorage
 */
public class MultiManagedStorage implements IManagedStorage {

    private final List<IManagedStorage> storages = new ArrayList<>();

    public MultiManagedStorage() {

    }

    public void attach(IManagedStorage storage) {
        clearCache();
        storages.add(storage);
    }

    public void detach(IManagedStorage storage) {
        clearCache();
        storages.remove(storage);
    }

    public void clearCache() {
        cacheFields.clear();
        cacheManaged = null;
        cacheNonLazyFields = null;
        cachePersistedFields = null;
        cacheSyncFields = null;
    }

    private final Map<ManagedKey, IRef> cacheFields = new HashMap<>();
    @Override
    public IRef getFieldByKey(ManagedKey key) {
        if (!cacheFields.containsKey(key)) {
            IRef ref = null;
            for (IManagedStorage storage : storages) {
                ref = storage.getFieldByKey(key);
                if (ref != null) {
                    break;
                }
            }
            cacheFields.put(key, ref);
        }
        return cacheFields.get(key);
    }

    private IManaged[] cacheManaged = null;
    @Override
    public IManaged[] getManaged() {
        if (cacheManaged == null) {
            cacheManaged = storages.stream().map(IManagedStorage::getManaged).flatMap(Arrays::stream).toArray(IManaged[]::new);
        }
        return cacheManaged;
    }

    private IRef[] cacheNonLazyFields = null;
    @Override
    public IRef[] getNonLazyFields() {
        if (cacheNonLazyFields == null) {
            cacheNonLazyFields = storages.stream().map(IManagedStorage::getNonLazyFields).flatMap(Arrays::stream).toArray(IRef[]::new);
        }
        return cacheNonLazyFields;
    }

    private IRef[] cachePersistedFields = null;
    @Override
    public IRef[] getPersistedFields() {
        if (cachePersistedFields == null) {
            cachePersistedFields = storages.stream().map(IManagedStorage::getPersistedFields).flatMap(Arrays::stream).toArray(IRef[]::new);
        }
        return cachePersistedFields;
    }

    private IRef[] cacheSyncFields = null;
    @Override
    public IRef[] getSyncFields() {
        if (cacheSyncFields == null) {
            cacheSyncFields = storages.stream().map(IManagedStorage::getSyncFields).flatMap(Arrays::stream).toArray(IRef[]::new);
        }
        return cacheSyncFields;
    }

    @Override
    public boolean hasDirtySyncFields() {
        for (IManagedStorage storage : storages) {
            if (storage.hasDirtySyncFields()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasDirtyPersistedFields() {
        for (IManagedStorage storage : storages) {
            if (storage.hasDirtyPersistedFields()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> ISubscription addSyncUpdateListener(ManagedKey key, IFieldUpdateListener<T> listener) {
        throw new IllegalStateException("do not add listener for multi managed storage");
    }

    public void removeAllSyncUpdateListener(ManagedKey key) {
        throw new IllegalStateException("do not remove listener for multi managed storage");
    }

    public boolean hasSyncListener(ManagedKey key) {
        for (IManagedStorage storage : storages) {
            if (storage.hasSyncListener(key)) {
                return true;
            }
        }
        return false;
    }

    public <T> Stream<Consumer<T>> notifyFieldUpdate(ManagedKey key, T currentValue) {
        return storages.stream().flatMap(storage -> storage.notifyFieldUpdate(key, currentValue));
    }

    @Override
    public void init() {
        storages.forEach(IManagedStorage::init);
    }

}
