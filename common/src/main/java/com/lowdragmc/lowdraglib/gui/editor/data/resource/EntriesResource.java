package com.lowdragmc.lowdraglib.gui.editor.data.resource;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.resource.EntriesResourceContainer;
import com.lowdragmc.lowdraglib.gui.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import javax.annotation.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

import static com.lowdragmc.lowdraglib.gui.editor.data.resource.EntriesResource.RESOURCE_NAME;


/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote TextureResource
 */
@LDLRegister(name = RESOURCE_NAME, group = "resource")
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
    public Tag serialize(String value) {
        return StringTag.valueOf(value);
    }

    @Override
    public String deserialize(Tag nbt) {
        return nbt instanceof StringTag stringTag ? stringTag.getAsString() : "missing value";
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        LocalizationUtils.appendDynamicLang(allResources()
                .map(entry -> Map.entry(getResourceName(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2)));
    }
}
