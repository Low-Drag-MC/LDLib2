package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

public class LangResource extends Resource<String> {

    public final static String RESOURCE_NAME = "ldlib.gui.editor.group.lang";

    @Override
    public IGuiTexture getIcon() {
        return Icons.TRANSLATE;
    }

    @Override
    public String getName() {
        return RESOURCE_NAME;
    }

    @Override
    public void buildDefault() {
    }

    @Override
    public void onLoad() {
        // TODO
//        LocalizationUtils.setResource(this);
    }

    @Override
    public void unLoad() {
        // TODO
//        LocalizationUtils.clearResource();
    }

    @Override
    public ResourceProviderContainer<String> createResourceContainer() {
        return super.createResourceContainer()
                .setOnAdd(() -> "value");
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
    public String getFileResourceSuffix() {
        return "lang.nbt";
    }

}
