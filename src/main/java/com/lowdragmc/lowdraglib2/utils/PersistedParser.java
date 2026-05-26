package com.lowdragmc.lowdraglib2.utils;

import com.google.common.base.Strings;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.DelegatingOpsAccessor;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.ManagedFieldUtils;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.network.connection.ConnectionType;

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
     * Creates a {@link MapCodec} for a specific type utilizing the provided {@link Supplier}.
     * This method internally constructs a codec through {@link PersistedParser#createCodec(Supplier)}
     * and wraps it to assume map-based data serialization.
     *
     * @param <T> The type of the object for which the {@link MapCodec} is created.
     * @param creator The supplier used to instantiate objects of type {@code T}.
     * @return A {@link MapCodec} for the specified type.
     */
    public static <T> MapCodec<T> createMapCodec(Supplier<T> creator) {
        return MapCodec.assumeMapUnsafe(createCodec(creator));
    }

    /**
     * This method is used to create a codec for the type serialized with {@link Persisted} or {@link Configurable} annotation.
     * @param creator The supplier to create the instance of the type.
     */
    public static <T> Codec<T> createCodec(Supplier<T> creator) {
        return new Codec<>() {
            @Override
            public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
                T instance = creator.get();
                HolderLookup.Provider provider = Platform.getFrozenRegistry();
                if (ops instanceof RegistryOps<T1> registryOps) {
                    provider = CommonHooks.extractLookupProvider(registryOps);
                }
                if (instance instanceof IPersistedSerializable persistedSerializable) {
                    CompoundTag tag;
                    if (input instanceof CompoundTag compoundTag) {
                        tag = compoundTag;
                    } else {
                        tag = (CompoundTag) ops.convertMap(NbtOps.INSTANCE, input);
                    }
                    persistedSerializable.deserializeNBT(provider, tag);
                } else {
                    deserialize(ops, input, instance, provider);
                }
                return DataResult.success(Pair.of(instance, input));
            }

            @Override
            public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
                HolderLookup.Provider provider = Platform.getFrozenRegistry();
                if (ops instanceof RegistryOps<T1> registryOps) {
                    provider = CommonHooks.extractLookupProvider(registryOps);
                }
                if (input instanceof IPersistedSerializable persistedSerializable) {
                    try {
                        var tag = persistedSerializable.serializeNBT(provider);
                        if (ops == NbtOps.INSTANCE || ops instanceof DelegatingOpsAccessor<?> accessor && accessor.getDelegate() == NbtOps.INSTANCE) {
                            return (DataResult<T1>) DataResult.success(tag);
                        }
                        return DataResult.success(NbtOps.INSTANCE.convertTo(ops, tag));
                    } catch (Exception e) {
                        return DataResult.error(e::getMessage);
                    }
                }
                return serialize(ops, input, provider);
            }

            @Override
            public String toString() {
                return "PersistedCodec";
            }
        };
    }

    /**
     * Creates a {@link StreamCodec} for serializing and deserializing objects of the specified type.
     * The provided {@link Supplier} is used to create new instances of the type during deserialization.
     * Serialization and deserialization employ internal stream operations.
     *
     * @param <T> The type of objects to be serialized and deserialized by the {@link StreamCodec}.
     * @param creator The {@link Supplier} responsible for creating new instances of the type {@code T} during deserialization.
     * @return A {@link StreamCodec} capable of handling serialization and deserialization of {@code T}-typed objects.
     */
    public static <T> StreamCodec<ByteBuf, T> createStreamCodec(Supplier<T> creator) {
        return StreamCodec.of((buf, value) -> {
            if (value instanceof IPersistedSerializable persistedSerializable) {
                persistedSerializable.writeToBuff(buf);
            } else {
                writeBuff(buf, value);
            }
        }, buf -> {
            T instance = creator.get();
            if (instance instanceof IPersistedSerializable persistedSerializable) {
                persistedSerializable.readFromBuff(buf);
            } else {
                readBuff(buf, instance);
            }
            return instance;
        });
    }

    /**
     * This method is used to serial the specific type data to the object fields with {@link Persisted} or {@link Configurable} annotation.
     */
    public static CompoundTag serializeNBT(Object object, HolderLookup.Provider provider) {
        return (CompoundTag) serialize(provider.createSerializationContext(NbtOps.INSTANCE), object, provider).result().orElse(new CompoundTag());
    }

    /**
     * This method is used to deserialize the NBT data to the object fields with {@link Persisted} or {@link Configurable} annotation.
     */
    public static void deserializeNBT(CompoundTag tag, Object object, HolderLookup.Provider provider) {
        deserialize(provider.createSerializationContext(NbtOps.INSTANCE), tag, object, provider);
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
     * Writes an object into the provided {@link ByteBuf} for serialization.
     * This method ensures that the object is serialized using a {@link RegistryFriendlyByteBuf},
     * which handles special registries and connection types.
     */
    public static void writeBuff(ByteBuf buf, Object object) {
        var provider = buf instanceof RegistryFriendlyByteBuf registryBuf ?
                registryBuf.registryAccess() : Platform.getFrozenRegistry();
        var registryBuf = buf instanceof RegistryFriendlyByteBuf rb ?
                rb : new RegistryFriendlyByteBuf(buf, provider, ConnectionType.NEOFORGE);
        writeStreamBuffInternal(true, registryBuf, object.getClass(), object, provider);
    }

    /**
     * This method is used to deserialize the specific type data to the object fields with {@link Persisted} or {@link Configurable} annotation.
     */
    public static <T> void deserialize(DynamicOps<T> op, T data, Object object, HolderLookup.Provider provider) {
        op.getMap(data).ifSuccess(map -> deserializeInternal(true, map, op, new HashMap<>(), object.getClass(), object, provider));
    }

    /**
     * Reads the serialized object data from the provided {@link ByteBuf}.
     * This method utilizes either the existing {@link RegistryFriendlyByteBuf}
     * or creates a new instance to handle registries and support specific
     * connection types. The deserialization is processed through an internal
     * utility method to populate object fields.
     */
    public static void readBuff(ByteBuf buf, Object object) {
        var provider = buf instanceof RegistryFriendlyByteBuf registryBuf ?
                registryBuf.registryAccess() : Platform.getFrozenRegistry();
        var registryBuf = buf instanceof RegistryFriendlyByteBuf rb ?
                rb : new RegistryFriendlyByteBuf(buf, provider, ConnectionType.NEOFORGE);
        readStreamBuffInternal(true, registryBuf, new HashMap<>(), object.getClass(), object, provider);
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
                var subFlatten = persistent.map(Configurable::subFlattenPersisted, Persisted::subFlattenPersisted);
                // sub configurable
                try {
                    field.setAccessible(true);
                    var value = field.get(object);
                    if (value != null) {
                        if (value instanceof INBTSerializable<?> serializable) {
                            var subData = (op == NbtOps.INSTANCE || op instanceof DelegatingOpsAccessor<?> accessor && accessor.getDelegate() == NbtOps.INSTANCE) ?
                                    (T) serializable.serializeNBT(provider) : 
                                    NbtOps.INSTANCE.convertTo(op, serializable.serializeNBT(provider));
                            if (subFlatten) {
                                var mapResult = op.getMap(subData);
                                if (mapResult.isSuccess()) {
                                    mapResult.getOrThrow().entries().forEachOrdered(entry ->
                                            recordBuilder.add(entry.getFirst(), entry.getSecond())
                                    );
                                } else {
                                    data = subData;
                                }
                            } else {
                                data = subData;
                            }
                        } else {
                            var builder = subFlatten ? recordBuilder : op.mapBuilder();
                            serializeInternal(true, builder, op, new HashMap<>(), ReflectionUtils.getRawType(field.getGenericType()), value, provider);
                            if (!subFlatten) {
                                data = builder.build(op.empty()).getOrThrow();
                            }
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
            if (persistent.map(Configurable::subConfigurable, Persisted::subPersisted)) {
                var subFlatten = persistent.map(Configurable::subFlattenPersisted, Persisted::subFlattenPersisted);
                // sub configurable
                try {
                    field.setAccessible(true);
                    var value = field.get(object);
                    if (value != null) {
                        if (data != null) {
                            if (value instanceof INBTSerializable serializable) {
                                if (op == NbtOps.INSTANCE || op instanceof DelegatingOpsAccessor<?> accessor && accessor.getDelegate() == NbtOps.INSTANCE) {
                                    serializable.deserializeNBT(provider, (Tag) data);
                                } else {
                                    serializable.deserializeNBT(provider, op.convertTo(NbtOps.INSTANCE, data));
                                }
                            } else {
                                op.getMap(data).ifSuccess(mapData -> deserializeInternal(true, mapData, op,
                                        new HashMap<>(), ReflectionUtils.getRawType(field.getGenericType()), value, provider));
                            }
                        } else if (subFlatten) {
                            if (value instanceof INBTSerializable serializable) {
                                if (op == NbtOps.INSTANCE || op instanceof DelegatingOpsAccessor<?> accessor && accessor.getDelegate() == NbtOps.INSTANCE) {
                                    serializable.deserializeNBT(provider, (Tag) op.createMap(map.entries()));
                                } else {
                                    serializable.deserializeNBT(provider, op.convertTo(NbtOps.INSTANCE, op.createMap(map.entries())));
                                }
                            } else {
                                deserializeInternal(true, map, op,
                                        new HashMap<>(), ReflectionUtils.getRawType(field.getGenericType()), value, provider);
                            }
                        }
                    }
                } catch (IllegalAccessException ignored) {}
            } else if (data != null) {
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

        // additional data
        if (root && object instanceof IPersistedSerializable serializable) {
            var additional = map.get("_additional");
            if (additional != null) {
                serializable.deserializeAdditionalNBT(op.convertTo(NbtOps.INSTANCE, additional), provider);
            }
            serializable.afterDeserialize();
        }
    }

    /**
     * Serializes the fields of the given object into a {@link RegistryFriendlyByteBuf} stream,
     * processing only the fields annotated with {@link Persisted} or {@link Configurable}.
     * This internal method performs recursive serialization, traversing the class hierarchy and handling special cases
     * for serializable interfaces and additional data handling.
     */
    private static void writeStreamBuffInternal(boolean root, RegistryFriendlyByteBuf buf, Class<?> clazz, Object object, HolderLookup.Provider provider) {
        if (clazz == Object.class || clazz == null) return;

        if (root && object instanceof IPersistedSerializable serializable) {
            serializable.beforeSerialize();
        }

        writeStreamBuffInternal(false, buf, clazz.getSuperclass(), object, provider);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            Either<Configurable, Persisted> persistent;
            if (field.isAnnotationPresent(Configurable.class)) {
                Configurable configurable = field.getAnnotation(Configurable.class);
                if (!configurable.persisted()) continue;
                persistent = Either.left(configurable);
            } else if (field.isAnnotationPresent(Persisted.class)) {
                persistent = Either.right(field.getAnnotation(Persisted.class));
            } else {
                continue;
            }

            if (persistent.map(Configurable::subConfigurable, Persisted::subPersisted)) {
                try {
                    field.setAccessible(true);
                    var value = field.get(object);
                    if (value != null) {
                        buf.writeBoolean(true);
                        if (value instanceof INBTSerializable<?> serializable) {
                            var tag = serializable.serializeNBT(provider);
                            buf.writeNbt(tag);
                        } else {
                            writeStreamBuffInternal(true, buf, ReflectionUtils.getRawType(field.getGenericType()), value, provider);
                        }
                    } else {
                        buf.writeBoolean(false);
                    }
                } catch (IllegalAccessException ignored) {}
            } else {
                ManagedFieldUtils.createKey(field).createRef(object).readSyncToStream(buf);
            }
        }

        if (root && object instanceof IPersistedSerializable serializable) {
            var additional = serializable.serializeAdditionalNBT(provider);
            if (additional != null && additional != EndTag.INSTANCE) {
                buf.writeBoolean(true);
                buf.writeNbt(additional);
            } else {
                buf.writeBoolean(false);
            }
            serializable.afterSerialize();
        }
    }

    /**
     * Deserializes the object's fields from the given {@link RegistryFriendlyByteBuf},
     * processing fields annotated with {@link Persisted} or {@link Configurable}.
     * Handles recursive deserialization across class hierarchies and manages
     * pre- and post-deserialization hooks for objects implementing {@link IPersistedSerializable}.
     * Additionally supports fields capable of serialization using {@link INBTSerializable}.
     */
    private static void readStreamBuffInternal(boolean root, RegistryFriendlyByteBuf buf, Map<String, Method> setters, Class<?> clazz, Object object, HolderLookup.Provider provider) {
        if (clazz == Object.class || clazz == null) return;

        if (root && object instanceof IPersistedSerializable serializable) {
            serializable.beforeDeserialize();
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ConfigSetter.class)) {
                ConfigSetter configSetter = method.getAnnotation(ConfigSetter.class);
                setters.putIfAbsent(configSetter.field(), method);
            }
        }

        readStreamBuffInternal(false, buf, setters, clazz.getSuperclass(), object, provider);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            Either<Configurable, Persisted> persistent;
            if (field.isAnnotationPresent(Configurable.class)) {
                Configurable configurable = field.getAnnotation(Configurable.class);
                if (!configurable.persisted()) continue;
                persistent = Either.left(configurable);
            } else if (field.isAnnotationPresent(Persisted.class)) {
                persistent = Either.right(field.getAnnotation(Persisted.class));
            } else {
                continue;
            }

            if (persistent.map(Configurable::subConfigurable, Persisted::subPersisted)) {
                try {
                    field.setAccessible(true);
                    var value = field.get(object);
                    if (buf.readBoolean() && value != null) {
                        if (value instanceof INBTSerializable serializable) {
                            serializable.deserializeNBT(provider, buf.readNbt(NbtAccounter.unlimitedHeap()));
                        } else {
                            readStreamBuffInternal(true, buf, new HashMap<>(), ReflectionUtils.getRawType(field.getGenericType()), value, provider);
                        }
                    }
                } catch (IllegalAccessException ignored) {}
            } else {
                ManagedFieldUtils.createKey(field).createRef(object).writeSyncFromStream(buf);
                Method setter = setters.get(field.getName());
                if (setter != null) {
                    setter.setAccessible(true);
                    field.setAccessible(true);
                    try {
                        setter.invoke(object, field.get(object));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        if (root && object instanceof IPersistedSerializable serializable) {
            if (buf.readBoolean()) {
                var tag = buf.readNbt(NbtAccounter.unlimitedHeap());
                if (tag == null) return;
                serializable.deserializeAdditionalNBT(tag, provider);
            }
            serializable.afterDeserialize();
        }
    }

}
