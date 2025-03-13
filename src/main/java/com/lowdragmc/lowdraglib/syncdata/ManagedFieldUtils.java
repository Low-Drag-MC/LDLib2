package com.lowdragmc.lowdraglib.syncdata;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.syncdata.annotation.*;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.field.RPCMethodMeta;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManagedFieldUtils {

    public static ManagedKey[] getManagedFields(Class<?> clazz) {
        List<ManagedKey> managedFields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.isAnnotationPresent(Persisted.class) || field.isAnnotationPresent(DescSynced.class)) {
                var managedKey = createKey(field);
                managedFields.add(managedKey);
            }
        }
        return managedFields.toArray(ManagedKey[]::new);
    }


    public static Map<String, RPCMethodMeta> getRPCMethods(Class<?> clazz) {
        Map<String, RPCMethodMeta> result = new HashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.isAnnotationPresent(RPCMethod.class)) {
                var rpcMethod = new RPCMethodMeta(method);
                result.put(rpcMethod.getName(), rpcMethod);
            }
        }
        return result;
    }

    public static ManagedKey createKey(Field field) {
        boolean isLazy = field.isAnnotationPresent(LazyManaged.class);
        boolean isDestSync = field.isAnnotationPresent(DescSynced.class);
        boolean isPersist = field.isAnnotationPresent(Persisted.class);
        boolean isDrop = field.isAnnotationPresent(DropSaved.class);
        boolean isReadOnlyManaged = field.isAnnotationPresent(ReadOnlyManaged.class);
        String name = field.getName();
        Type type = field.getGenericType();
        var managedKey = new ManagedKey(name, isDestSync, isPersist, isDrop, isLazy, type, field);

        if (isPersist) {
            var persisted = field.getAnnotation(Persisted.class);
            managedKey.setPersistentKey(persisted.key());
        }

        if (isReadOnlyManaged) {
            var readOnlyManaged = field.getAnnotation(ReadOnlyManaged.class);
            var clazz = field.getDeclaringClass();
            var rawType = field.getType();
            try {
                var onDirtyMethod = clazz.getDeclaredMethod(readOnlyManaged.onDirtyMethod(), rawType);
                var serializeMethod = clazz.getDeclaredMethod(readOnlyManaged.serializeMethod(), rawType);
                var deserializeMethod = clazz.getDeclaredMethod(readOnlyManaged.deserializeMethod(), CompoundTag.class);
                onDirtyMethod.setAccessible(true);
                serializeMethod.setAccessible(true);
                deserializeMethod.setAccessible(true);
                managedKey.setRedOnlyManaged(onDirtyMethod, serializeMethod, deserializeMethod);
            } catch (NoSuchMethodException e) {
                LDLib.LOGGER.warn("No such methods for @ReadOnlyManaged field {}", field);
            }
        }
        return managedKey;
    }

    public record FieldRefs(IRef[] syncedRefs, IRef[] persistedRefs, IRef[] nonLazyFields,
                            Map<ManagedKey, IRef> fieldRefMap) {

    }

    public interface FieldChangedCallback {
        void onFieldChanged(IRef ref, int index, boolean changed);
    }


    public static FieldRefs getFieldRefs(ManagedKey[] keys, Object obj, FieldChangedCallback syncFieldChangedCallback, FieldChangedCallback persistedFieldChangedCallback) {
        List<IRef> syncedFields = new ArrayList<>();
        List<IRef> persistedFields = new ArrayList<>();
        List<IRef> nonLazyFields = new ArrayList<>();
        Map<ManagedKey, IRef> fieldRefMap = new HashMap<>();
        for (ManagedKey key : keys) {
            final var fieldObj = key.createRef(obj);
            fieldObj.markAsDirty();
            fieldRefMap.put(key, fieldObj);
            if (!fieldObj.getKey().isLazy()) {
                nonLazyFields.add(fieldObj);
            }
            int syncIndex = -1;
            int persistIndex = -1;
            if (key.isDestSync()) {
                syncIndex = syncedFields.size();
                syncedFields.add(fieldObj);
            }
            if (key.isPersist()) {
                persistIndex = persistedFields.size();
                persistedFields.add(fieldObj);
            }
            int finalSyncIndex = syncIndex;
            int finalPersistIndex = persistIndex;
            fieldObj.setOnSyncListener((changed) -> syncFieldChangedCallback.onFieldChanged(fieldObj, finalSyncIndex, changed));
            fieldObj.setOnPersistedListener((changed) -> persistedFieldChangedCallback.onFieldChanged(fieldObj, finalPersistIndex, changed));
        }
        return new FieldRefs(
                syncedFields.toArray(IRef[]::new),
                persistedFields.toArray(IRef[]::new),
                nonLazyFields.toArray(IRef[]::new),
                fieldRefMap
        );
    }

}
