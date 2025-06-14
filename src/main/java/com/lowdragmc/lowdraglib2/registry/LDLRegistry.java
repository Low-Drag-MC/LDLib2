package com.lowdragmc.lowdraglib2.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public abstract class LDLRegistry<K, V> implements Iterable<V> {
    public static final Map<ResourceLocation, LDLRegistry<?, ?>> REGISTERED = new LinkedHashMap<>();

    protected final BiMap<K, V> registry;
    @Getter
    protected final ResourceLocation registryName;
    @Getter
    protected boolean frozen = false;

    public LDLRegistry(ResourceLocation registryName) {
        registry = initRegistry();
        this.registryName = registryName;

        REGISTERED.put(registryName, this);
    }

    protected BiMap<K, V> initRegistry() {
        return HashBiMap.create();
    }

    public boolean containKey(K key) {
        return registry.containsKey(key);
    }

    public boolean containValue(V value) {
        return registry.containsValue(value);
    }

    public void freeze() {
        if (frozen) {
            throw new IllegalStateException("Registry is already frozen!");
        }

        this.frozen = true;
    }

    public void unfreeze() {
        if (!frozen) {
            throw new IllegalStateException("Registry is already unfrozen!");
        }

        this.frozen = false;
    }

    public void register(K key, V value) {
        if (frozen) {
            throw new IllegalStateException("[register] registry %s has been frozen".formatted(registryName));
        }
        if (containKey(key)) {
            throw new IllegalStateException("[register] registry %s contains key %s already".formatted(registryName, key));
        }
        registry.put(key, value);
    }

    @Nullable
    public V replace(K key, V value) {
        if (frozen) {
            throw new IllegalStateException("[replace] registry %s has been frozen".formatted(registryName));
        }
        if (!containKey(key)) {
            LDLib2.LOGGER.warn("[replace] couldn't find key %s in registry %s".formatted(registryName, key));
        }
        return registry.put(key, value);
    }

    public V registerOrOverride(K key, V value) {
        if (frozen) {
            throw new IllegalStateException("[register] registry %s has been frozen".formatted(registryName));
        }
        return registry.put(key, value);
    }

    @NotNull
    @Override
    public Iterator<V> iterator() {
        return registry.values().iterator();
    }

    public Set<V> values() {
        return registry.values();
    }

    public Set<K> keys() {
        return registry.keySet();
    }

    public Set<Map.Entry<K, V>> entries() {
        return registry.entrySet();
    }

    public Map<K, V> registry() {
        return registry;
    }

    @Nullable
    public V get(K key) {
        return registry.get(key);
    }

    public Optional<V> getOptional(K key) {
        return Optional.ofNullable(get(key));
    }

    public V getOrDefault(K key, V defaultValue) {
        return registry.getOrDefault(key, defaultValue);
    }

    public K getKey(V value) {
        return registry.inverse().get(value);
    }

    public K getOrDefaultKey(V key, K defaultKey) {
        return registry.inverse().getOrDefault(key, defaultKey);
    }

    public abstract void writeBuf(V value, RegistryFriendlyByteBuf buf);

    @Nullable
    public abstract V readBuf(RegistryFriendlyByteBuf buf);

    public abstract Tag saveToNBT(V value);

    @Nullable
    public abstract V loadFromNBT(Tag tag);

    public boolean remove(K name) {
        return registry.remove(name) != null;
    }

    public abstract Codec<V> codec();

    public abstract Codec<Optional<V>> optionalCodec();

    public abstract StreamCodec<RegistryFriendlyByteBuf, V> streamCodec();

    //************************ Built-in Registry ************************//

    public static class String<V> extends LDLRegistry<java.lang.String, V> {

        public String(ResourceLocation registryName) {
            super(registryName);
        }

        @Override
        public void writeBuf(V value, RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(containValue(value));
            if (containValue(value)) {
                buf.writeUtf(getKey(value));
            }
        }

        @Override
        public V readBuf(RegistryFriendlyByteBuf buf) {
            if (buf.readBoolean()) {
                return get(buf.readUtf());
            }
            return null;
        }

        @Override
        public Tag saveToNBT(V value) {
            if (containValue(value)) {
                return StringTag.valueOf(getKey(value));
            }
            return new CompoundTag();
        }

        @Override
        public V loadFromNBT(Tag tag) {
            return get(tag.getAsString());
        }

        @Override
        public Codec<V> codec() {
            return Codec.STRING.flatXmap(str -> Optional.ofNullable(this.get(str)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.registryName + ": " + str)), obj -> Optional.ofNullable(this.getKey(obj)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown registry element in " + this.registryName + ": " + obj)));
        }

        @Override
        public Codec<Optional<V>> optionalCodec() {
            return Codec.STRING.flatXmap(str -> DataResult.success(getOptional(str)),
                    optional -> optional.map(obj -> DataResult.success(this.getKey(obj))).orElseGet(() -> DataResult.error(() -> "registry key in " + this.registryName)));
        }

        public StreamCodec<RegistryFriendlyByteBuf, V> streamCodec() {
            return StreamCodec.of((buf, value) -> buf.writeUtf(getKey(value)), buf -> Objects.requireNonNull(get(buf.readUtf())));
        }

    }

    public static class RL<V> extends LDLRegistry<ResourceLocation, V> {

        public RL(ResourceLocation registryName) {
            super(registryName);
        }

        @Override
        public void writeBuf(V value, RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(containValue(value));
            if (containValue(value)) {
                buf.writeUtf(getKey(value).toString());
            }
        }

        @Override
        public V readBuf(RegistryFriendlyByteBuf buf) {
            if (buf.readBoolean()) {
                return get(ResourceLocation.parse(buf.readUtf()));
            }
            return null;
        }

        @Override
        public Tag saveToNBT(V value) {
            if (containValue(value)) {
                return StringTag.valueOf(getKey(value).toString());
            }
            return new CompoundTag();
        }

        @Override
        public V loadFromNBT(Tag tag) {
            return get(ResourceLocation.parse(tag.getAsString()));
        }

        @Override
        public Codec<V> codec() {
            return ResourceLocation.CODEC.flatXmap(rl -> Optional.ofNullable(this.get(rl)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.registryName + ": " + rl)), obj -> Optional.ofNullable(this.getKey(obj)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown registry element in " + this.registryName + ": " + obj)));
        }

        @Override
        public Codec<Optional<V>> optionalCodec() {
            return ResourceLocation.CODEC.flatXmap(rl -> DataResult.success(getOptional(rl)),
                    optional -> optional.map(obj -> DataResult.success(this.getKey(obj))).orElseGet(() -> DataResult.error(() -> "registry key in " + this.registryName)));
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, V> streamCodec() {
            return StreamCodec.of((buf, value) -> buf.writeResourceLocation(getKey(value)), buf -> Objects.requireNonNull(get(buf.readResourceLocation())));
        }
    }
}
