package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.io.File;

public class TexturesResource extends Resource<IGuiTexture> {
    public final FileResourceProvider<IGuiTexture> global;

    public TexturesResource() {
        var builtinResource = new BuiltinResourceProvider<>(this);
        builtinResource.addResource("empty", IGuiTexture.EMPTY);
        builtinResource.addResource("missing", IGuiTexture.MISSING_TEXTURE);
        addResourceProvider(builtinResource);
        addResourceProvider(global = createNewFileResourceProvider(new File(LDLib.getAssetsDir(), "ldlib/resources")));
        global.setName("global");
    }

    @Override
    public void buildDefault() {
    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.PICTURE;
    }

    @Override
    public String getName() {
        return "texture";
    }

    @Override
    public Tag serialize(IGuiTexture value, HolderLookup.Provider provider) {
        return IGuiTexture.CODEC.encodeStart(NbtOps.INSTANCE, value).result().orElse(null);
    }

    @Override
    public IGuiTexture deserialize(Tag nbt, HolderLookup.Provider provider) {
        return IGuiTexture.CODEC.parse(NbtOps.INSTANCE, nbt).result().orElse(IGuiTexture.MISSING_TEXTURE);
    }

    @Override
    public boolean canRemoveResourceProvider(ResourceProvider<IGuiTexture> provider) {
        return provider != global && super.canRemoveResourceProvider(provider);
    }

    @Override
    public ResourceProviderContainer<IGuiTexture> createResourceProviderContainer(ResourceProvider<IGuiTexture> provider) {
        var container = super.createResourceProviderContainer(provider)
                .setUiSupplier(path -> new UIElement().layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                }).style(style -> style.backgroundTexture(provider.getResource(path))));
        if (provider.supportAdd()) {
            container.setOnMenu((c, m) -> m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
                for (var holder : LDLibRegistries.GUI_TEXTURES) {
                    String name = holder.annotation().name();
                    if (name.equals("empty") || name.equals("ui_resource_texture")) continue;
                    IGuiTexture icon = holder.value().get();
                    menu.leaf(icon, name, () -> c.addNewResource(holder.value().get()));
                }
            }));
        }
        return container;
    }
}
