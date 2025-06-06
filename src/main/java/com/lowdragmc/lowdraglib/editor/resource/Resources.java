package com.lowdragmc.lowdraglib.editor.resource;

import com.google.common.collect.ImmutableMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote Resource
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Resources implements INBTSerializable<CompoundTag> {
    public static final Resources EMPTY = new Resources(Map.of());

    public final ImmutableMap<String, Resource<?>> resources;

    public Resources(Map<String, Resource<?>> resources) {
        this.resources = ImmutableMap.copyOf(resources);
    }

    public static Resources of(Resource<?>... resources) { // default
        Map<String, Resource<?>> map = new LinkedHashMap<>();
        for (Resource<?> resource : resources) {
            map.put(resource.getName(), resource);
        }
        return new Resources(map);
    }

    /**
     * Prepare the default resources.
     */
    public void buildDefault() {
        resources.values().forEach(Resource::buildDefault);
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        resources.forEach((key, resource) -> tag.put(key, resource.serializeNBT(provider)));
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        resources.forEach((k, v) -> v.deserializeNBT(provider, nbt.getCompound(k)));
    }


}
