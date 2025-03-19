package com.lowdragmc.lowdraglib.editor.data.resource;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.editor.ui.resource.EntriesResourceContainer;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import javax.annotation.Nullable;

import static com.lowdragmc.lowdraglib.editor.data.resource.EntriesResource.RESOURCE_NAME;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote TextureResource
 */
@LDLRegister(name = RESOURCE_NAME, registry = "ldlib:resource")
public class EntriesResource extends Resource<String> {

    public final static String RESOURCE_NAME = "ldlib.gui.editor.group.entries";

    @Override
    public String name() {
        return RESOURCE_NAME;
    }

    @Override
    public void buildDefault() {
        addBuiltinResource("ldlib.author", "Hello KilaBash!");
    }

    @Override
    public void onLoad() {
        LocalizationUtils.setResource(this);
    }

    @Override
    public void unLoad() {
        LocalizationUtils.clearResource();
    }

    @Override
    public ResourceContainer<String, ?> createContainer(ResourcePanel panel) {
        return new EntriesResourceContainer(this, panel);
    }

    @Nullable
    @Override
    public Tag serialize(String value, HolderLookup.Provider provider) {
        return StringTag.valueOf(value);
    }

    @Override
    public String deserialize(Tag nbt, HolderLookup.Provider provider) {
        return nbt instanceof StringTag stringTag ? stringTag.getAsString() : "missing value";
    }

    @Override
    public void deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        super.deserializeNBT(nbt, provider);
        LocalizationUtils.appendDynamicLang(allResources()
                .map(entry -> Map.entry(getResourceName(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2)));
    }
}
