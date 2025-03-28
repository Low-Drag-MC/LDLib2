package com.lowdragmc.lowdraglib.editor.data.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceContainer;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.registry.ILDLRegisterClient;
import com.mojang.datafixers.util.Either;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

 import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote Resource
 * You can register a new global resource (available for all projects) using {@link LDLRegister},
 * or you can add a resource dynamically to the project
 */
@SuppressWarnings({"unchecked"})
public abstract class Resource<T> implements ILDLRegisterClient<Resource<?>, Supplier<Resource>> {
    private final static Map<String, StaticResource<?>> STATIC_RESOURCES = new HashMap<>();
    @Getter
    private final Map<String, T> builtinResources = new LinkedHashMap<>();
    @Nullable
    @Getter
    private final File staticLocation;
    // runtime
    @Nullable
    private StaticResource<T> staticResource;

    public Resource() {
        this.staticLocation = null;
    }

    public Resource(@Nullable File staticLocation) {
        this.staticLocation = staticLocation;
    }

    public void buildDefault() {
    }

    public void onLoad() {
        getStaticResource().loadAndUpdateStaticResource();
    }

    public void unLoad() {

    }

    public StaticResource<T> getStaticResource() {
        if (!supportStaticResource()) return StaticResource.empty();
        if (staticResource == null) {
            staticResource = (StaticResource<T>) STATIC_RESOURCES.computeIfAbsent(name(),
                    name -> LDLibRegistries.RESOURCES.getOptional(name)
                            .map(r -> new StaticResource<T>(r.value().get()))
                            .orElseGet(StaticResource::empty));
        }
        return staticResource;
    }

    public T removeBuiltinResource(String key) {
        return builtinResources.remove(key);
    }

    public T removeStaticResource(File file) {
        if (getStaticResource().staticResources.containsKey(file)) {
            getStaticResource().staticResourcesLastModified.remove(file);
        }
        if (file.isFile() && file.exists()) {
            try {
                file.delete();
            } catch (Exception e) {
                LDLib.LOGGER.error("Failed to delete static resource file {} from {}: ", file, this, e);
            }
        }
        return getStaticResource().staticResources.remove(file);
    }

    public T removeResource(Either<String, File> key) {
        return key.map(this::removeBuiltinResource, this::removeStaticResource);
    }

    public boolean hasBuiltinResource(String key) {
        return builtinResources.containsKey(key);
    }

    public boolean hasStaticResource(File file) {
        return getStaticResource().staticResources.containsKey(file);
    }

    public boolean hasResource(Either<String, File> key) {
        return key.map(this::hasBuiltinResource, this::hasStaticResource);
    }

    public void addBuiltinResource(String key, T resource) {
        if (supportBuiltInResource()) {
            builtinResources.put(key, resource);
        }
    }

    public void addResource(Either<String, File> key, T resource) {
        if (key.left().isPresent()) {
            addBuiltinResource(key.left().get(), resource);
        } else if (key.right().isPresent()) {
            addStaticResource(key.right().get(), resource);
        }
    }

    public void addStaticResource(File file, T resource) {
        if (supportStaticResource()) {
            var data = serialize(resource, Platform.getFrozenRegistry());
            if (data == null) return;
            var fileData = new CompoundTag();
            fileData.put("data", data);
            fileData.putString("type", name());
            try {
                NbtIo.write(fileData, file.toPath());
                getStaticResource().staticResources.put(file, resource);
                getStaticResource().staticResourcesLastModified.put(file, file.lastModified());
            } catch (IOException e) {
                LDLib.LOGGER.error("Failed to save static resource file {} from {}: ", file, this, e);
            }
        }
    }

    public T getBuiltinResource(String key) {
        return builtinResources.get(key);
    }

    public T getStaticResource(File file) {
        return getStaticResource().staticResources.get(file);
    }

    public T getResource(Either<String, File> key) {
        return key.map(this::getBuiltinResource, this::getStaticResource);
    }

    public T getBuiltinResourceOrDefault(String key, T defaultValue) {
        return builtinResources.getOrDefault(key, defaultValue);
    }

    public T getStaticResourceOrDefault(File file, T defaultValue) {
        return getStaticResource().staticResources.getOrDefault(file, defaultValue);
    }

    public T getResourceOrDefault(Either<String, File> key, T defaultValue) {
        return key.map(k -> getBuiltinResourceOrDefault(k, defaultValue), f -> getStaticResourceOrDefault(f, defaultValue));
    }

    public void merge(Resource<T> resource) {
        resource.builtinResources.forEach((k, v) -> {
            if (!this.builtinResources.containsKey(k)) {
                this.builtinResources.put(k, v);
            }
        });
    }

    public Stream<Map.Entry<Either<String, File>, T>> allResources() {
        return Stream.concat(
                getStaticResource().staticResources.entrySet().stream()
                        .map(entry -> Map.entry(Either.right(entry.getKey()), entry.getValue())),
                builtinResources.entrySet().stream()
                        .map(entry -> Map.entry(Either.left(entry.getKey()), entry.getValue()))
        );
    }

    public abstract String name();

    public abstract ResourceContainer<T, ? extends Widget> createContainer(ResourcePanel panel);

    @Nullable
    public abstract Tag serialize(T value, HolderLookup.Provider provider);
    public abstract T deserialize(Tag nbt, HolderLookup.Provider provider);

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        builtinResources.forEach((key, value) -> {
            var nbt = serialize(value, provider);
            if (nbt != null) {
                tag.put(key, nbt);
            }
        });
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        builtinResources.clear();
        for (String key : nbt.getAllKeys()) {
            builtinResources.put(key, deserialize(nbt.get(key), provider));
        }
    }

    public String getResourceName(Either<String, File> key) {
        return key.map(k -> k, this::getStaticResourceName);
    }

    ///  STATIC RESOURCE
    public boolean supportBuiltInResource() {
        return true;
    }

    public boolean supportStaticResource() {
        return staticLocation != null && (staticLocation.isDirectory() || staticLocation.mkdirs());
    }

    public String getStaticResourceSuffix() {
        return ".nbt";
    }

    public File getStaticResourceFile(String name) {
        return new File(staticLocation, name + getStaticResourceSuffix());
    }

    public String getStaticResourceName(File file) {
        return file.getName().substring(0, file.getName().length() - getStaticResourceSuffix().length());
    }

    /**
     * Load and update static resource
     * @return true static resource changes.
     */
    public boolean loadAndUpdateStaticResource() {
        if (supportStaticResource()) {
            return getStaticResource().loadAndUpdateStaticResource();
        }
        return false;
    }

    @Override
    public String toString() {
        return name();
    }

}
