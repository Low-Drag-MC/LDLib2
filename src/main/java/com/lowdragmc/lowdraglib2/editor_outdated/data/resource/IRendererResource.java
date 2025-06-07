package com.lowdragmc.lowdraglib2.editor_outdated.data.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib2.editor_outdated.ui.ResourcePanel;
import com.lowdragmc.lowdraglib2.editor_outdated.ui.resource.IRendererResourceContainer;
import com.lowdragmc.lowdraglib2.editor_outdated.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class IRendererResource extends Resource<IRenderer> {
    public final static String RESOURCE_NAME = "ldlib.gui.editor.group.renderer";

    public IRendererResource() {
        super(new File(LDLib2.getAssetsDir(), "ldlib/resources/renderers"));
        addBuiltinResource("empty", IRenderer.EMPTY);
    }

    @Override
    public void buildDefault() {
        addBuiltinResource("furnace", new IModelRenderer(ResourceLocation.parse("block/furnace")));
    }

    @Override
    public String name() {
        return RESOURCE_NAME;
    }

    @Override
    public ResourceContainer<IRenderer, ? extends Widget> createContainer(ResourcePanel resourcePanel) {
        return new IRendererResourceContainer(this, resourcePanel);
    }

    @Nullable
    @Override
    public Tag serialize(IRenderer renderer, HolderLookup.Provider provider) {
        return renderer.serializeWrapper();
    }

    @Override
    public IRenderer deserialize(Tag tag, HolderLookup.Provider provider) {
        return IRenderer.deserializeWrapper(tag);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        getBuiltinResources().clear();
        addBuiltinResource("empty", IRenderer.EMPTY);
        for (String key : nbt.getAllKeys()) {
            addBuiltinResource(key, deserialize(nbt.get(key), provider));
        }
    }
}
