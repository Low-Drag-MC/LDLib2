package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import org.jetbrains.annotations.Nullable;
import java.io.File;

@KJSBindings
public class UIResource extends Resource<UITemplate> {
    public static final UIResource INSTANCE = new UIResource();

    public UIResource() {
    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.WIDGET_BASIC;
    }

    @Override
    public String getName() {
        return "ui";
    }

    @Override
    public void buildBuiltin(ResourceInstance<UITemplate> resourceInstance) {
        var global = new FileResourceProvider<>(resourceInstance, new File(LDLib2.getAssetsDir(), "ldlib2/resources/global"));
        global.setName("global");
        resourceInstance.addBuiltinProvider(global);
    }

    @Nullable
    @Override
    public Tag serializeResource(UITemplate value, HolderLookup.Provider provider) {
        return UITemplate.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), value).result().orElse(null);
    }

    @Override
    public UITemplate deserializeResource(Tag nbt, HolderLookup.Provider provider) {
        return UITemplate.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), nbt).result().orElse(UITemplate.missing());
    }

    @Override
    public ResourceProviderContainer<UITemplate> createResourceProviderContainer(IResourceProvider<UITemplate> provider) {
        return new UIResourceProviderContainer(provider);
    }
}
