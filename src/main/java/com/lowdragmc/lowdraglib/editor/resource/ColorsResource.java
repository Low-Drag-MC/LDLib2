package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

public class ColorsResource extends Resource<Integer> {

    public final static String RESOURCE_NAME = "ldlib.gui.editor.group.colors";

    public ColorsResource() {

    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.COLOR;
    }

    @Override
    public void buildDefault() {
    }

    @Override
    public String getName() {
        return RESOURCE_NAME;
    }

    @Nullable
    @Override
    public Tag serialize(Integer value, HolderLookup.Provider provider) {
        return IntTag.valueOf(value);
    }

    @Override
    public Integer deserialize(Tag nbt, HolderLookup.Provider provider) {
        return nbt instanceof IntTag intTag ? intTag.getAsInt() : -1;
    }

    @Override
    public String getFileResourceSuffix() {
        return ".color.nbt";
    }

    @Override
    public ResourceProviderContainer<Integer> createResourceContainer() {
        return super.createResourceContainer().setUiSupplier(key -> new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        }).style(style -> style.backgroundTexture(new ColorRectTexture(getResourceOrDefault(key, -1)))))
                .setOnAdd(() -> -1);
    }
}
