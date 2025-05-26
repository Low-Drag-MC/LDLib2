package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.*;

import com.lowdragmc.lowdraglib.editor.ui.view.ResourceView;
import net.minecraft.network.chat.Component;

public abstract class Resource<T> {
    protected final List<ResourceProvider<T>> providers = new ArrayList<>();

    public Resource() {
    }

    /**
     * Generate default resources.
     */
    public void buildDefault() {
    }

    public abstract IGuiTexture getIcon();

    /**
     * Resource name, it can also be used to obtain the resource from the resource view. also see {@link ResourceView#getResourceByName(String)}
     */
    public abstract String getName();

    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    /**
     * Serialize resource to nbt for persistence.
     */
    @Nullable
    public abstract Tag serialize(T value, HolderLookup.Provider provider);

    /**
     * Deserialize resource from nbt.
     */
    @Nullable
    public abstract T deserialize(Tag nbt, HolderLookup.Provider provider);


    @Nullable
    public CompoundTag serializeNBT(T value, HolderLookup.Provider provider) {
        var tag = serialize(value, provider);
        if (tag == null) return null;
        var nbt = new CompoundTag();
        nbt.put("data", tag);
        nbt.putString("type", getName());
        return nbt;
    }

    @Nullable
    public T deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.getString("type").equals(getName())) {
            return deserialize(nbt.get("data"), provider);
        }
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

}
