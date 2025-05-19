package com.lowdragmc.lowdraglib.utils;

import com.google.common.base.Strings;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.ManagedFieldUtils;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import lombok.experimental.UtilityClass;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This is a tool class to serialize and deserialize the object fields with {@link Persisted} or {@link Configurable} annotation.
 */
@UtilityClass
public final class PersistedParser {

    /**
     * This method is used to create a codec for the type serialized with {@link Persisted} or {@link Configurable} annotation.
     * @param creator The supplier to create the instance of the type.
     */
    public static <T> Codec<T> createCodec(Supplier<T> creator) {
        return new Codec<>() {
            @Override
            public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
                T instance = creator.get();
                deserialize(ops, input, instance, Platform.getFrozenRegistry());
                return DataResult.success(Pair.of(instance, ops.empty()));
            }

            @Override
            public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
                return serialize(ops, input, Platform.getFrozenRegistry());
            }

            @Override
            public String toString() {
                return "PersistedCodec";
            }
        };
    }

    /**
     * This method is used to serial the specific type data to the object fields with {@link Persisted} or {@link Configurable} annotation.
     */
    public static CompoundTag serializeNBT(Object object, HolderLookup.Provider provider) {
        return (CompoundTag) serialize(NbtOps.INSTANCE, object, provider).result().orElse(new CompoundTag());
    }

    /**
     * This method is used to deserialize the NBT data to the object fields with {@link Persisted} or {@link Configurable} annotation.
     */
    public static void deserializeNBT(CompoundTag tag, Object object, HolderLookup.Provider provider) {
        deserialize(NbtOps.INSTANCE, tag, object, provider);
    }

    /**
     * This method is used to serialize the object fields with {@link Persisted} or {@link Configurable} annotation to specific type data.
     */
    public static <T> DataResult<T> serialize(DynamicOps<T> op, Object object, HolderLookup.Provider provider) {
        var builder = op.mapBuilder();
        serializeInternal(true, builder, op, object.getClass(), object, provider);
        return builder.build(op.empty());
    }

    /**
     * This method is used to deserialize the specific type data to the object fields with {@link Persisted} or {@link Configurable} annotation.
     */
    public static <T> void deserialize(DynamicOps<T> op, T data, Object object, HolderLookup.Provider provider) {
        op.getMap(data).ifSuccess(map -> deserializeInternal(true, map, op, new HashMap<>(), object.getClass(), object, provider));
    }

    /**
     * This method is used to serialize the object fields with {@link Persisted} or {@link Configurable} annotation to the op data.
     */
    private static <T> void serializeInternal(boolean root, RecordBuilder<T> recordBuilder, DynamicOps<T> op, Class<?> clazz, Object object, HolderLookup.Provider provider) {
        if (clazz == Object.class || clazz == null) return;

        if (root && object instanceof IPersistedSerializable serializable) {
            serializable.beforeSerialize();
        }

        serializeInternal(false, recordBuilder, op, clazz.getSuperclass(), object, provider);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String key = field.getName();

            if (field.isAnnotationPresent(Configurable.class)) {
                Configurable configurable = field.getAnnotation(Configurable.class);
                if (!configurable.persisted()) {
                    continue;
                } else if (!Strings.isNullOrEmpty(configurable.key())) {
                    key = configurable.key();
                }
            } else if (field.isAnnotationPresent(Persisted.class)) {
                Persisted persisted = field.getAnnotation(Persisted.class);
                if (!Strings.isNullOrEmpty(persisted.key())) {
                    key = persisted.key();
                }
            } else {
                continue;
            }

            T data = null;
            // sub configurable
            if ((field.isAnnotationPresent(Configurable.class) && field.getAnnotation(Configurable.class).subConfigurable()) ||
                    (field.isAnnotationPresent(Persisted.class) && field.getAnnotation(Persisted.class).subPersisted())) {
                try {
                    field.setAccessible(true);
                    var value = field.get(object);
                    if (value != null) {
                        if (value instanceof INBTSerializable<?> serializable) {
                            data = op == NbtOps.INSTANCE ? (T) serializable.serializeNBT(provider) : NbtOps.INSTANCE.convertTo(op, serializable.serializeNBT(provider));
                        } else {
                            var builder = op.mapBuilder();
                            serializeInternal(false, builder, op, ReflectionUtils.getRawType(field.getGenericType()), value, provider);
                            data = builder.build(op.empty()).getOrThrow();
                        }
                    }
                } catch (IllegalAccessException ignored) {}
            } else {
                data = ManagedFieldUtils.createKey(field).createRef(object).readPersisted(op);
            }
            if (data != null) {
                recordBuilder.add(key, data);
            }
        }

        // additional data
        if (root && object instanceof IPersistedSerializable serializable) {
            var additional = serializable.serializeAdditionalNBT(provider);
            if (additional != null && additional != EndTag.INSTANCE) {
                var data = NbtOps.INSTANCE.convertTo(op, additional);
                recordBuilder.add("_additional", data);
            }
            serializable.afterSerialize();
        }
    }

    /**
     * This method is used to deserialize the op data to the object fields with {@link Persisted} or {@link Configurable} annotation.
     */
    private static <T> void deserializeInternal(boolean root, MapLike<T> map, DynamicOps<T> op, Map<String, Method> setters, Class<?> clazz, Object object, HolderLookup.Provider provider) {
        if (clazz == Object.class || clazz == null) return;

        if (root && object instanceof IPersistedSerializable serializable) {
            serializable.beforeDeserialize();
        }

        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(ConfigSetter.class)) {
                ConfigSetter configSetter = method.getAnnotation(ConfigSetter.class);
                String name = configSetter.field();
                if (!setters.containsKey(name)) {
                    setters.put(name, method);
                }
            }
        }

        deserializeInternal(false, map, op, setters, clazz.getSuperclass(), object, provider);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String key = field.getName();

            if (field.isAnnotationPresent(Configurable.class)) {
                Configurable configurable = field.getAnnotation(Configurable.class);
                if (!configurable.persisted()) {
                    continue;
                } else if (!Strings.isNullOrEmpty(configurable.key())) {
                    key = configurable.key();
                }
            } else if (field.isAnnotationPresent(Persisted.class)) {
                Persisted persisted = field.getAnnotation(Persisted.class);
                if (!Strings.isNullOrEmpty(persisted.key())) {
                    key = persisted.key();
                }
            } else {
                continue;
            }

            T data = map.get(key);
            // sub configurable
            if (data != null) {
                if ((field.isAnnotationPresent(Configurable.class) && field.getAnnotation(Configurable.class).subConfigurable()) ||
                        (field.isAnnotationPresent(Persisted.class) && field.getAnnotation(Persisted.class).subPersisted())) {
                    try {
                        field.setAccessible(true);
                        var value = field.get(object);
                        if (value != null) {
                            if (value instanceof INBTSerializable serializable) {
                                if (op == NbtOps.INSTANCE) {
                                    serializable.deserializeNBT(provider, (Tag) data);
                                } else {
                                    serializable.deserializeNBT(provider, op.convertTo(NbtOps.INSTANCE, data));
                                }
                            } else {
                                op.getMap(data).ifSuccess(mapData -> deserializeInternal(true, mapData, op, new HashMap<>(), ReflectionUtils.getRawType(field.getGenericType()), value, provider));
                            }
                        }
                    } catch (IllegalAccessException ignored) {}
                } else {
                    ManagedFieldUtils.createKey(field).createRef(object).writePersisted(op, data);
                    Method setter = setters.get(field.getName());

                    if (setter != null) {
                        field.setAccessible(true);
                        try {
                            setter.invoke(object, field.get(object));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        // additional data
        if (root && object instanceof IPersistedSerializable serializable) {
            var additional = map.get("_additional");
            if (additional != null) {
                serializable.deserializeAdditionalNBT(op.convertTo(NbtOps.INSTANCE, additional), provider);
            }
            serializable.afterDeserialize();
        }
    }

}
