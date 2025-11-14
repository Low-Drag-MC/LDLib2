package com.lowdragmc.lowdraglib2.utils;

import com.google.common.base.Strings;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.ManagedFieldUtils;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.mojang.datafixers.util.Either;
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
                if (instance instanceof IPersistedSerializable persistedSerializable) {
                    CompoundTag tag;
                    if (input instanceof CompoundTag compoundTag) {
                        tag = compoundTag;
                    } else {
                        tag = (CompoundTag) ops.convertMap(NbtOps.INSTANCE, input);
                    }
                    persistedSerializable.deserializeNBT(Platform.getFrozenRegistry(), tag);
                } else {
                    deserialize(ops, input, instance, Platform.getFrozenRegistry());
                }
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
        serializeInternal(true, builder, op, new HashMap<>(), object.getClass(), object, provider);
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
    private static <T> void serializeInternal(boolean root, RecordBuilder<T> recordBuilder, DynamicOps<T> op, Map<String, Method> skipValues, Class<?> clazz, Object object, HolderLookup.Provider provider) {
        if (clazz == Object.class || clazz == null) return;

        if (root && object instanceof IPersistedSerializable serializable) {
            serializable.beforeSerialize();
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(SkipPersistedValue.class)) {
                SkipPersistedValue skipPersistedValue = method.getAnnotation(SkipPersistedValue.class);
                String name = skipPersistedValue.field();
                if (!skipValues.containsKey(name)) {
                    skipValues.put(name, method);
                }
            }
        }

        serializeInternal(false, recordBuilder, op, skipValues, clazz.getSuperclass(), object, provider);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String key = field.getName();
            Either<Configurable, Persisted> persistent;
            if (field.isAnnotationPresent(Configurable.class)) {
                Configurable configurable = field.getAnnotation(Configurable.class);
                if (!configurable.persisted()) {
                    continue;
                } else if (!Strings.isNullOrEmpty(configurable.key())) {
                    key = configurable.key();
                }
                persistent = Either.left(configurable);
            } else if (field.isAnnotationPresent(Persisted.class)) {
                Persisted persisted = field.getAnnotation(Persisted.class);
                if (!Strings.isNullOrEmpty(persisted.key())) {
                    key = persisted.key();
                }
                persistent = Either.right(persisted);
            } else {
                continue;
            }

            var skipMethod = skipValues.get(field.getName());
            if (skipMethod != null) {
                skipMethod.setAccessible(true);
                field.setAccessible(true);
                try {
                    if (skipMethod.invoke(object, field.get(object)) instanceof Boolean skip && skip) {
                        continue;
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }

            T data = null;
            if (persistent.map(Configurable::subConfigurable, Persisted::subPersisted)) {
                // sub configurable
                try {
                    field.setAccessible(true);
                    var value = field.get(object);
                    if (value != null) {
                        if (value instanceof INBTSerializable<?> serializable) {
                            data = op == NbtOps.INSTANCE ? 
                                    (T) serializable.serializeNBT(provider) : 
                                    NbtOps.INSTANCE.convertTo(op, serializable.serializeNBT(provider));
                        } else {
                            var builder = op.mapBuilder();
                            serializeInternal(false, builder, op, new HashMap<>(), ReflectionUtils.getRawType(field.getGenericType()), value, provider);
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

        for (Method method : clazz.getDeclaredMethods()) {
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

            Either<Configurable, Persisted> persistent;
            if (field.isAnnotationPresent(Configurable.class)) {
                Configurable configurable = field.getAnnotation(Configurable.class);
                if (!configurable.persisted()) {
                    continue;
                } else if (!Strings.isNullOrEmpty(configurable.key())) {
                    key = configurable.key();
                }
                persistent = Either.left(configurable);
            } else if (field.isAnnotationPresent(Persisted.class)) {
                Persisted persisted = field.getAnnotation(Persisted.class);
                if (!Strings.isNullOrEmpty(persisted.key())) {
                    key = persisted.key();
                }
                persistent = Either.right(persisted);
            } else {
                continue;
            }

            T data = map.get(key);
            if (data != null) {
                if (persistent.map(Configurable::subConfigurable, Persisted::subPersisted)) {
                    // sub configurable
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
                                op.getMap(data).ifSuccess(mapData -> deserializeInternal(true, mapData, op,
                                        new HashMap<>(), ReflectionUtils.getRawType(field.getGenericType()), value, provider));
                            }
                        }
                    } catch (IllegalAccessException ignored) {}
                } else {
                    ManagedFieldUtils.createKey(field).createRef(object).writePersisted(op, data);
                    Method setter = setters.get(field.getName());

                    if (setter != null) {
                        setter.setAccessible(true);
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
