package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.io.File;

public class LangResource extends Resource<String> {
    public final FileResourceProvider<String> global;

    public LangResource() {
        var builtinResource = new BuiltinResourceProvider<>(this);
        builtinResource.addResource("ldlib.author", "KilaBash");
        addResourceProvider(builtinResource);
        addResourceProvider(global = createNewFileResourceProvider(new File(LDLib.getAssetsDir(), "ldlib/resources")));
        global.setName("global");
    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.TRANSLATE;
    }

    @Override
    public String getName() {
        return "lang";
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
    public boolean canRemoveResourceProvider(ResourceProvider<String> provider) {
        return provider != global && super.canRemoveResourceProvider(provider);
    }

}
