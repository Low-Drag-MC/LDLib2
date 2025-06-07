package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.io.File;

public class TexturesResource extends Resource<IGuiTexture> {

    public TexturesResource() {
        var builtinResource = new BuiltinResourceProvider<>(this);
        builtinResource.addResource("empty", IGuiTexture.EMPTY);
        builtinResource.addResource("missing", IGuiTexture.MISSING_TEXTURE);
        addResourceProvider(builtinResource);
    }

    @Override
    public void buildDefault() {
        addResourceProvider(createNewFileResourceProvider(new File(LDLib2.getAssetsDir(), "ldlib/resources")).setName("global"));
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
    public Tag serializeResource(IGuiTexture value, HolderLookup.Provider provider) {
        return IGuiTexture.CODEC.encodeStart(NbtOps.INSTANCE, value).result().orElse(null);
    }

    @Override
    public IGuiTexture deserializeResource(Tag nbt, HolderLookup.Provider provider) {
        return IGuiTexture.CODEC.parse(NbtOps.INSTANCE, nbt).result().orElse(IGuiTexture.MISSING_TEXTURE);
    }

    @Override
    public ResourceProviderContainer<IGuiTexture> createResourceProviderContainer(ResourceProvider<IGuiTexture> provider) {
        var container = super.createResourceProviderContainer(provider)
                .setUiSupplier(path -> new UIElement().layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                }).style(style -> style.backgroundTexture(provider.getResource(path))));
        container.setOnEdit((c, path) -> {
            var texture = provider.getResource(path);
            if (texture == null) return;
            c.getEditor().inspectorView.inspect(texture, configurator -> c.markResourceDirty(path), null);
        });
        if (provider.supportAdd()) {
            container.setOnMenu((c, m) -> m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
                for (var holder : LDLib2Registries.GUI_TEXTURES) {
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
