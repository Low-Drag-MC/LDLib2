package com.lowdragmc.lowdraglib.gui.editor.data;

import com.lowdragmc.lowdraglib.gui.editor.data.resource.ColorsResource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.EntriesResource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.TexturesResource;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
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
            var resource = AnnotationDetector.REGISTER_RESOURCES.stream()
                    .filter(wrapper -> wrapper.annotation().name().equals(key))
                    .findFirst();
            if (resource.isEmpty()) continue;
            map.put(key, resource.get().creator().get());
        }
        var resources = new Resources(map);
        resources.deserializeNBT(tag);
        return resources;
    }

    @Deprecated(since = "1.21")
    public static Resources defaultResource() { // default
        var resources = of(new EntriesResource(), new ColorsResource(), new TexturesResource());
        resources.resources.values().forEach(Resource::buildDefault);
        return resources;
    }

    public static Resources of(Resource<?>... resources) { // default
        Map<String, Resource<?>> map = new LinkedHashMap<>();
        for (Resource<?> resource : resources) {
            map.put(resource.name(), resource);
        }
        return new Resources(map);
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

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        resources.forEach((key, resource) -> tag.put(key, resource.serializeNBT()));
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        resources.forEach((k, v) -> v.deserializeNBT(nbt.getCompound(k)));
    }


}
