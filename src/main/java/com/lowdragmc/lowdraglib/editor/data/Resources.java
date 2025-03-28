package com.lowdragmc.lowdraglib.editor.data;

import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.editor.data.resource.Resource;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote Resource
 */
public class Resources {

    public final Map<String, Resource<?>> resources;

    public Resources(Map<String, Resource<?>> resources) {
        this.resources = resources;
    }

    public static Resources emptyResource() {
        return new Resources(new LinkedHashMap<>());
    }

    public static Resources fromNBT(CompoundTag tag) {
        Map<String, Resource<?>> map = new LinkedHashMap<>();
        for (String key : tag.getAllKeys()) {
            LDLibRegistries.RESOURCES.getOptional(key).ifPresent(resource -> map.put(key, resource.value().get()));
        }
        var resources = new Resources(map);
        resources.deserializeNBT(tag, Platform.getFrozenRegistry());
        return resources;
    }

    public static Resources of(Resource<?>... resources) { // default
        Map<String, Resource<?>> map = new LinkedHashMap<>();
        for (Resource<?> resource : resources) {
            map.put(resource.name(), resource);
        }
        return new Resources(map);
    }

    public static Resources ofDefault(Resource<?>... resources) {
        var result = of(resources);
        result.resources.values().forEach(Resource::buildDefault);
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void merge(Resources resources) {
        this.resources.forEach((k, v) -> {
            if (resources.resources.containsKey(k)) {
                Resource f = resources.resources.get(k);
                v.merge(f);
            }
        });
    }

    public void load() {
        resources.values().forEach(Resource::onLoad);
    }

    public void dispose() {
        resources.values().forEach(Resource::unLoad);
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        resources.forEach((key, resource) -> tag.put(key, resource.serializeNBT(provider)));
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        resources.forEach((k, v) -> v.deserializeNBT(nbt.getCompound(k), provider));
    }


}
