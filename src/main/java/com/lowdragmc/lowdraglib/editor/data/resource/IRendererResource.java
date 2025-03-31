package com.lowdragmc.lowdraglib.editor.data.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.editor.ui.resource.IRendererResourceContainer;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static com.lowdragmc.lowdraglib.editor.data.resource.IRendererResource.RESOURCE_NAME;

@LDLRegisterClient(name = RESOURCE_NAME, registry = "ldlib:resource")
public class IRendererResource extends Resource<IRenderer> {
    public final static String RESOURCE_NAME = "ldlib.gui.editor.group.renderer";

    public IRendererResource() {
        super(new File(LDLib.getAssetsDir(), "ldlib/resources/renderers"));
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
