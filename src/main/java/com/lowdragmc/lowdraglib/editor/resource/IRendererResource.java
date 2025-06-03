package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class IRendererResource extends Resource<IRenderer> {
    public final FileResourceProvider<IRenderer> global;

    public IRendererResource() {
        var builtinResource = new BuiltinResourceProvider<>(this);
        builtinResource.addResource("empty", IRenderer.EMPTY);
        addResourceProvider(builtinResource);
        addResourceProvider(global = createNewFileResourceProvider(new File(LDLib.getAssetsDir(), "ldlib/resources")));
        global.setName("global");
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
        return "renderer";
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
    public boolean canRemoveResourceProvider(ResourceProvider<IRenderer> provider) {
        return provider != global && super.canRemoveResourceProvider(provider);
    }

    @Override
    public ResourceProviderContainer<IRenderer> createResourceProviderContainer(ResourceProvider<IRenderer> provider) {
        var container = super.createResourceProviderContainer(provider);
//                .setUiSupplier(path -> new UIElement().layout(layout -> {
//                    layout.setWidthPercent(100);
//                    layout.setHeightPercent(100);
//                }).style(style -> style.backgroundTexture(provider.getResource(path))))
        if (provider.supportAdd()) {
            container.setOnMenu((c, m) -> m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
                for (var holder : LDLibRegistries.RENDERERS) {
                    var name = holder.annotation().name();
                    menu.leaf(name, () -> {
                        var renderer = holder.value().get();
                        renderer.initRenderer();
                        c.addNewResource(renderer);
                    });
                }
            }));
        }
        return container;
    }

}
