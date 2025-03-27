package com.lowdragmc.lowdraglib.editor.data.resource;

import com.lowdragmc.lowdraglib.registry.ILDLRegister;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote Resource
 * You can register a new global resource (available for all projects) using {@link LDLRegister},
 * or you can add a resource dynamically to the project
 */
public abstract class Resource<T> implements ILDLRegister<Resource<?>, Supplier<Resource>> {
    @Getter
    protected final Map<String, T> data = new LinkedHashMap<>();

    public void buildDefault() {

    }

    public void onLoad() {

    }

    public void unLoad() {

    }

    public T removeResource(String key) {
        return data.remove(key);
    }

    public boolean hasResource(String key) {
        return data.containsKey(key);
    }

    public void addResource(String key, T resource) {
        data.put(key, resource);
    }

    public Set<Map.Entry<String, T>> allResources() {
        return data.entrySet();
    }

    public T getResource(String key) {
        return data.get(key);
    }

    public T getResourceOrDefault(String key, T defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    public void merge(Resource<T> resource) {
        resource.data.forEach((k, v) -> {
            if (!this.data.containsKey(k)) {
                this.data.put(k, v);
            }
        });
    }

    public abstract String name();

    public abstract ResourceContainer<T, ? extends Widget> createContainer(ResourcePanel panel);

    @Nullable
    public abstract Tag serialize(T value, HolderLookup.Provider provider);
    public abstract T deserialize(Tag nbt, HolderLookup.Provider provider);

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        data.forEach((key, value) -> {
            var nbt = serialize(value, provider);
            if (nbt != null) {
                tag.put(key, nbt);
            }
        });
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        data.clear();
        for (String key : nbt.getAllKeys()) {
            data.put(key, deserialize(nbt.get(key), provider));
        }
    }

}
