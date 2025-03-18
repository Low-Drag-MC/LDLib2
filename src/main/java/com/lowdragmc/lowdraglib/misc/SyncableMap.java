package com.lowdragmc.lowdraglib.misc;

import com.lowdragmc.lowdraglib.syncdata.*;
import com.lowdragmc.lowdraglib.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.DirectAccessor;
import com.lowdragmc.lowdraglib.syncdata.managed.ManagedHolder;
import com.lowdragmc.lowdraglib.syncdata.payload.PrimitiveTypedPayload;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * SyncableMap is a map that can be synced to the client.
 * Use anonymous class to create a SyncableMap.
 * @param <K>
 * @param <V>
 */
public abstract class SyncableMap<K, V> implements Map<K, V>, IContentChangeAware, INBTSerializable<Tag> {

    private final IAccessor keyAccessor;
    private final IAccessor valueAccessor;

    private final Class<?> keyType;
    private final Class<?> valueType;

    private boolean stringKey = false;

    public SyncableMap() {
        var clazz = getClass();
        var parent = clazz.getGenericSuperclass();
        var keyType = ((ParameterizedType) parent).getActualTypeArguments()[0];
        var valueType = ((ParameterizedType) parent).getActualTypeArguments()[1];

        this.keyType = ReflectionUtils.getRawType(keyType, Object.class);
        this.valueType = ReflectionUtils.getRawType(valueType, Object.class);

        stringKey = keyType == String.class;

        keyAccessor = TypedPayloadRegistries.findByType(keyType);
        valueAccessor = TypedPayloadRegistries.findByType(valueType);

        if (keyAccessor == null || valueAccessor == null) {
            throw new RuntimeException("Cannot find accessor for key or value type");
        }

        if(keyAccessor.isReadOnly()) {
            throw new RuntimeException("Key accessor is not managed");
        }

        if(valueAccessor.isReadOnly()) {
            throw new RuntimeException("Value accessor is not managed");
        }
    }

    private final Map<K, V> map = new HashMap<>();

    @Setter @Getter
    private Runnable onContentsChanged = () -> {};

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        var result = map.put(key, value);
        onContentsChanged.run();
        return result;
    }

    @Override
    public V remove(Object key) {
        var result = map.remove(key);
        onContentsChanged.run();
        return result;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        map.putAll(m);
        onContentsChanged.run();
    }

    @Override
    public void clear() {
        map.clear();
        onContentsChanged.run();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return map.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    private static Tag readVal(DirectAccessor accessor, Object val, HolderLookup.Provider provider) {
        return val == null ? PrimitiveTypedPayload.ofNull().serializeNBT(provider) : accessor.readManagedField(AccessorOp.PERSISTED, ManagedHolder.of(val), provider).serializeNBT(provider);
    }

    private static Object writeVal(DirectAccessor accessor, Tag val, Class<?> type, HolderLookup.Provider provider) {
        if (val == null) {
            return null;
        }
        var holder = ManagedHolder.ofType(type);
        var payload = TypedPayloadRegistries.create(accessor.getDefaultType());
        payload.deserializeNBT(val, provider);
        accessor.writeManagedField(AccessorOp.PERSISTED, holder, payload, provider);
        return holder.value();
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        if(stringKey) {
            var tag = new CompoundTag();
            for (var entry : map.entrySet()) {
                var valueTag = readVal((DirectAccessor) valueAccessor, entry.getValue(), provider);
                tag.put((String) entry.getKey(), valueTag);
            }
            return tag;
        }
        var list = new ListTag();
        map.forEach((k, v) -> {
            var tag = new CompoundTag();
            var keyTag = readVal((DirectAccessor) keyAccessor, k, provider);
            var valueTag = readVal((DirectAccessor) valueAccessor, v, provider);
            tag.put("k", keyTag);
            if (valueTag != null) {
                tag.put("v", valueTag);
            }
            list.add(tag);
        });
        return list;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        map.clear();

        if(nbt instanceof CompoundTag tag) {
            for (var key : tag.getAllKeys()) {
                var valueTag = tag.get(key);
                var value = writeVal((DirectAccessor) valueAccessor, valueTag, valueType, provider);
                map.put((K) key, (V) value);
            }
            return;
        }

        ((ListTag) nbt).forEach(tag -> {
            var compound = (CompoundTag) tag;
            var keyTag = compound.get("k");
            var valueTag = compound.get("v");
            var key = writeVal((DirectAccessor) keyAccessor, keyTag, keyType, provider);
            var value = writeVal((DirectAccessor) valueAccessor, valueTag, valueType, provider);
            map.put((K) key, (V) value);
        });
    }
}
