package com.lowdragmc.lowdraglib2.syncdata.field;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.ManagedFieldUtils;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCMethodMeta;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is used to store all the fields of a class that implements {@link IManaged} and all the RPC methods{@link RPCMethodMeta}.
 * <br>
 * You don't need to create this class for all instances.
 * Create a static instance of this class in the class that implements {@link IManaged} and
 * return it in the {@link IManaged#getFieldHolder()} method.
 */
public final class ManagedFieldHolder {
    public final static ManagedFieldHolder EMPTY = new ManagedFieldHolder();

    private final Map<String, ManagedKey> fieldNameMap = new HashMap<>();
    private ManagedKey[] fields;
    private Map<String, RPCMethodMeta> rpcMethodMap = new HashMap<>();

    private ManagedFieldHolder() {}

    /**
     * @param clazz the class to get the sync field keys from
     */
    public ManagedFieldHolder(Class<? extends IManaged> clazz) {
        this.initAll(clazz);
    }

    /**
     * merge the sync field keys from the given class
     *
     * @param clazz  the class to get the sync field keys from
     * @param parent the parent class to get the sync field keys from
     */
    public ManagedFieldHolder(Class<? extends IManaged> clazz, ManagedFieldHolder parent) {
        this(clazz);
        merge(parent);
    }

    public void merge(ManagedFieldHolder other) {
        this.fields = ArrayUtils.addAll(this.fields, other.fields);
        this.resetSyncFieldIndexMap();
        this.rpcMethodMap.putAll(other.rpcMethodMap);
    }


    private void initAll(Class<? extends IManaged> clazz) {
        this.fields = ManagedFieldUtils.getManagedFields(clazz);
        resetSyncFieldIndexMap();
        this.rpcMethodMap = ManagedFieldUtils.getRPCMethods(clazz);
    }

    private void resetSyncFieldIndexMap() {
        fieldNameMap.clear();
        for (ManagedKey key : fields) {
            if (fieldNameMap.containsKey(key.getName())) {
                LDLib2.LOGGER.warn("Duplicate sync field name: " + key.getName());
                continue;
            }
            fieldNameMap.put(key.getName(), key);
        }
    }

    public ManagedKey[] getFields() {
        return fields;
    }

    public Map<String, RPCMethodMeta> getRpcMethodMap() {
        return rpcMethodMap;
    }

    public ManagedKey getSyncedFieldIndex(String name) {
        if (!fieldNameMap.containsKey(name)) {
            throw new IllegalArgumentException("No sync field with name " + name);
        }
        return fieldNameMap.get(name);
    }

    /// GenerateHolder Automatically
    private final static Map<Class<? extends IManaged>, ManagedFieldHolder> CLASS_MANAGED_FIELD_HOLDERS = new ConcurrentHashMap<>();

    public static ManagedFieldHolder ofCache(Class<? extends IManaged> clazz) {
        // quick check
        ManagedFieldHolder holder = CLASS_MANAGED_FIELD_HOLDERS.get(clazz);
        if (holder != null) {
            return holder;
        }

        ManagedFieldHolder newHolder;
        var superClazz = clazz.getSuperclass();
        if (IManaged.class.isAssignableFrom(superClazz)) {
            newHolder = new ManagedFieldHolder(clazz, ofCache((Class<? extends IManaged>) superClazz));
        } else {
            newHolder = new ManagedFieldHolder(clazz);
        }

        ManagedFieldHolder existing = CLASS_MANAGED_FIELD_HOLDERS.putIfAbsent(clazz, newHolder);
        return existing != null ? existing : newHolder;
    }

    public static void clearCache() {
        CLASS_MANAGED_FIELD_HOLDERS.clear();
    }
}
