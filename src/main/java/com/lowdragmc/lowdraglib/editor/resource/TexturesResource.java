package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public class TexturesResource extends Resource<IGuiTexture> {
    public final static String RESOURCE_NAME = "ldlib.gui.editor.group.textures";

    public TexturesResource() {
        addResource(IResourcePath.builtin("empty"), IGuiTexture.EMPTY);
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
        return RESOURCE_NAME;
    }

    @Override
    public ResourceProviderContainer<IGuiTexture> createResourceContainer() {
        return super.createResourceContainer().setUiSupplier(key -> new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        }).style(style -> style.backgroundTexture(getResourceOrDefault(key, IGuiTexture.EMPTY))))
                .setOnMenu((container, m) -> m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
                    for (var holder : LDLibRegistries.GUI_TEXTURES) {
                        String name = holder.annotation().name();
                        if (name.equals("empty") || name.equals("ui_resource_texture")) continue;
                        IGuiTexture icon = holder.value().get();
                        menu.leaf(icon, name, () -> container.addNewResource(holder.value().get()));
                    }
                }));
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
    public String getFileResourceSuffix() {
        return "texture.nbt";
    }

    @Override
    public void onLoad() {
        super.onLoad();
//        UIResourceTexture.RESOURCE.set(this);
    }

    @Override
    public void unLoad() {
        super.unLoad();
//        UIResourceTexture.RESOURCE.remove();
    }
}
