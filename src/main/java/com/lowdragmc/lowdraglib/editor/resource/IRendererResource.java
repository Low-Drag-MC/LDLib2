package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class IRendererResource extends Resource<IRenderer> {
    public final static String RESOURCE_NAME = "ldlib.gui.editor.group.renderer";

    public IRendererResource() {
        addResource(IResourcePath.builtin("empty"), IRenderer.EMPTY);
    }

    @Override
    public void buildDefault() {
    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.MODEL;
    }

    @Override
    public String getName() {
        return RESOURCE_NAME;
    }

    public ResourceProviderContainer<IRenderer> createResourceContainer() {
        return super.createResourceContainer();
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
    public String getFileResourceSuffix() {
        return "renderer.nbt";
    }
}
