package com.lowdragmc.lowdraglib.syncdata.field;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.ManagedFieldUtils;
import com.lowdragmc.lowdraglib.syncdata.rpc.RPCMethodMeta;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to store all the fields of a class that implements {@link IManaged} and all the RPC methods{@link RPCMethodMeta}.
 * <br>
 * You don't need to create this class for all instances.
 * Create a static instance of this class in the class that implements {@link IManaged} and
 * return it in the {@link IManaged#getFieldHolder()} method.
 */
public final class ManagedFieldHolder {

    private final Map<String, ManagedKey> fieldNameMap = new HashMap<>();
    private ManagedKey[] fields;
    private Map<String, RPCMethodMeta> rpcMethodMap = new HashMap<>();

    /**
     * @param clazz the class to get the sync field keys from
     */
    public ManagedFieldHolder(Class<? extends IManaged> clazz) {
        this.clazz = clazz;
        this.initAll();
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

    private final Class<? extends IManaged> clazz;

    private void initAll() {
        this.fields = ManagedFieldUtils.getManagedFields(clazz);
        resetSyncFieldIndexMap();
        this.rpcMethodMap = ManagedFieldUtils.getRPCMethods(clazz);
    }

    private void resetSyncFieldIndexMap() {
        fieldNameMap.clear();
        for (ManagedKey key : fields) {
            if (fieldNameMap.containsKey(key.getName())) {
                LDLib.LOGGER.warn("Duplicate sync field name: " + key.getName());
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
}
