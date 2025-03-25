package com.lowdragmc.lowdraglib.syncdata.field;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.syncdata.*;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib.syncdata.ref.IRef;
import net.minecraft.Util;
import org.apache.commons.lang3.function.Consumers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FieldManagedStorage implements IManagedStorage {

    private final IManaged owner;

    private BitSet dirtySyncFields;
    private BitSet dirtyPersistedFields;

    private IRef[] syncFields;

    private boolean initialized = false;
    private IRef[] persistedFields;
    private IRef[] nonLazyFields;
    private Map<ManagedKey, IRef> fieldMap;

    private final ReentrantLock lock = new ReentrantLock();

    private final Map<ManagedKey, List<FieldUpdateSubscription>> listeners = new HashMap<>();

    public <T> ISubscription addSyncUpdateListener(ManagedKey key, IFieldUpdateListener<T> listener) {
        var subscription = new FieldUpdateSubscription(key, listener) {
            @Override
            public void unsubscribe() {
                listeners.getOrDefault(key, new ArrayList<>()).remove(this);
            }
        };
        listeners.computeIfAbsent(key, k -> new ArrayList<>()).add(subscription);
        return subscription;
    }

    public void removeAllSyncUpdateListener(ManagedKey key) {
        listeners.remove(key);
    }

    public boolean hasSyncListener(ManagedKey key) {
        var list = listeners.get(key);
        return list != null && !list.isEmpty();
    }

    public <T> Stream<Consumer<T>> notifyFieldUpdate(ManagedKey key, T currentValue) {
        var list = listeners.get(key);
        if (list != null) {
            return list.stream()
                    .map(sub -> (IFieldUpdateListener<T>) sub.listener)
                    .map(listener -> {
                        try {
                            return listener.onFieldUpdated(key, currentValue);
                        } catch (Throwable t) {
                            LDLib.LOGGER.error("Error occurred while notifying field {} update", key, t);
                        }
                        return Consumers.nop();
                    });
        }
        return Stream.empty();
    }

    public void init() {
        lock.lock();
        try {
            if (initialized) {
                return;
            }
            ManagedKey[] fields = owner.getFieldHolder().getFields();

            var result = ManagedFieldUtils.getFieldRefs(fields, owner, (ref, index, changed) -> {
                if (dirtySyncFields != null && index >= 0) {
                    dirtySyncFields.set(index, changed);
                    owner.onSyncMarkChanged(ref, changed);
                }
            }, (ref, index, changed) -> {
                if (dirtyPersistedFields != null && index >= 0) {
                    dirtyPersistedFields.set(index, changed);
                    owner.onPersistedMarkChanged(ref, changed);
                }
            });

            syncFields = result.syncedRefs();
            persistedFields = result.persistedRefs();

            dirtySyncFields = new BitSet(syncFields.length);
            dirtyPersistedFields = new BitSet(result.persistedRefs().length);

            nonLazyFields = result.nonLazyFields();
            fieldMap = result.fieldRefMap();
            initialized = true;
            if (LDLib.isClient()) {
                initEnhancedFeature();
            }
        } finally {
            lock.unlock();
        }
    }

    public FieldManagedStorage(IManaged owner) {
        this.owner = owner;
    }

    public IRef[] getSyncFields() {
        init();
        return syncFields;
    }

    @Override
    public boolean hasDirtySyncFields() {
        return !dirtySyncFields.isEmpty();
    }

    @Override
    public boolean hasDirtyPersistedFields() {
        return !dirtyPersistedFields.isEmpty();
    }

    public IRef[] getPersistedFields() {
        init();
        return persistedFields;
    }

    @Override
    public IManaged[] getManaged() {
        return new IManaged[]{owner};
    }

    public IRef getFieldByKey(ManagedKey key) {
        init();
        return fieldMap.get(key);
    }

    public IRef[] getNonLazyFields() {
        init();
        return nonLazyFields;
    }

    final static BiFunction<Field, Class<?>, Method> METHOD_CACHES = Util.memoize((rawField, clazz) -> {
        var methodName = rawField.getAnnotation(UpdateListener.class).methodName();
        Method method = null;
        while (clazz != null && method == null) {
            try {
                method = clazz.getDeclaredMethod(methodName, rawField.getType(), rawField.getType());
                method.setAccessible(true);
            } catch (NoSuchMethodException ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        if (method == null) {
            LDLib.LOGGER.error("couldn't find the listener method {} for synced field {}", methodName, rawField.getName());
        }
        return method;
    });

    public void initEnhancedFeature() {
        for (IRef syncField : getSyncFields()) {
            var rawField = syncField.getKey().getRawField();
            if (rawField.isAnnotationPresent(RequireRerender.class) && owner instanceof IEnhancedManaged enhancedManaged) {
                addSyncUpdateListener(syncField.getKey(),  );
            }
            if (rawField.isAnnotationPresent(UpdateListener.class)) {
                final var method = METHOD_CACHES.apply(rawField, owner.getClass());
                if (method != null) {
                    addSyncUpdateListener(syncField.getKey(), (name, newValue, oldValue) -> {
                        try {
                            method.invoke(owner, newValue, oldValue);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }
}
