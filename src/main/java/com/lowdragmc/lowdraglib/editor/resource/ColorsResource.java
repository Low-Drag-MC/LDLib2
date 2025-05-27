package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.io.File;

public class ColorsResource extends Resource<Integer> {
    public final FileResourceProvider<Integer> global;

    public ColorsResource() {
        var builtinResource = new BuiltinResourceProvider<>(this);
        for (ColorPattern value : ColorPattern.values()) {
            builtinResource.addResource(value.colorName, value.color);
        }
        addResourceProvider(builtinResource);
        addResourceProvider(global = createNewFileResourceProvider(new File(LDLib.getAssetsDir(), "ldlib/resources")));
        global.setName("global");
    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.COLOR;
    }

    @Override
    public String getName() {
        return "color";
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
    public boolean canRemoveResourceProvider(ResourceProvider<Integer> provider) {
        return provider != global && super.canRemoveResourceProvider(provider);
    }

    @Override
    public ResourceProviderContainer<Integer> createResourceProviderContainer(ResourceProvider<Integer> provider) {
        return super.createResourceProviderContainer(provider)
                .setAddDefault(() -> -1)
                .setUiSupplier(path -> new UIElement().layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                }).style(style -> style.backgroundTexture(new ColorRectTexture(provider.getResource(path)))));
    }
}
