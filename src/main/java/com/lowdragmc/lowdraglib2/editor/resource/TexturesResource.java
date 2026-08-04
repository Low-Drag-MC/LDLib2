package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.util.Locale;
import java.util.Set;

@KJSBindings
public class TexturesResource extends Resource<IGuiTexture> {
    public static final TexturesResource INSTANCE = new TexturesResource();
    /** Image formats Minecraft's texture manager can load, and so the ones a drop can be turned into. */
    public static final Set<String> IMAGE_EXTENSIONS = Set.of(".png");

    @Override
    public void buildBuiltin(ResourceInstance<IGuiTexture> resourceInstance) {
        super.buildBuiltin(resourceInstance);
//        resourceInstance.addBuiltinProvider(Sprites.getProvider(resourceInstance));
    }

    @Override
    public void buildBuiltin(BuiltinResourceProvider<IGuiTexture> provider) {
        provider.addResource("empty", IGuiTexture.EMPTY);
        provider.addResource("missing", IGuiTexture.MISSING_TEXTURE);
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
    public boolean canImportFile(File file) {
        return super.canImportFile(file) || file.isFile() && IMAGE_EXTENSIONS.stream()
                .anyMatch(extension -> file.getName().toLowerCase(Locale.ROOT).endsWith(extension));
    }

    @Override
    public void importFile(ResourceImportContext<IGuiTexture> context) {
        var file = context.getFile();
        if (super.canImportFile(file)) {
            super.importFile(context);
            return;
        }
        // an image only becomes usable once Minecraft can address it, so one from outside the pack has
        // to be copied in first. The texture manager reads the file when it is first drawn, so unlike a
        // model this needs no resource reload.
        ResourceFileImport.resolveOrImport(context.getOwner(), file, "textures",
                location -> context.complete(SpriteTexture.of(location)),
                context::cancel);
    }

    @Override
    public Tag serializeResource(IGuiTexture value, HolderLookup.Provider provider) {
        return IGuiTexture.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), value).result().orElse(null);
    }

    @Override
    public IGuiTexture deserializeResource(Tag nbt, HolderLookup.Provider provider) {
        return IGuiTexture.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), nbt).result().orElse(IGuiTexture.MISSING_TEXTURE);
    }

    @Override
    public ResourceProviderContainer<IGuiTexture> createResourceProviderContainer(IResourceProvider<IGuiTexture> provider) {
        var container = super.createResourceProviderContainer(provider)
                .setUiSupplier(path -> new UIElement().layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                }).style(style -> style.backgroundTexture(provider.getResource(path))));
        container.setOnEdit((c, path) -> {
            var texture = provider.getResource(path);
            if (texture == null) return;
            c.getEditor().inspectorView.inspect(texture, configurator -> c.markResourceDirty(path));
        });
        container.setOnDragProvider(UIResourceTexture::new);
        if (provider.supportAdd()) {
            container.setOnCreateMenu((c, m) -> m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
                for (var holder : LDLib2Registries.GUI_TEXTURES) {
                    String name = holder.annotation().name();
                    if (name.equals("empty") || name.equals("missing") || name.equals("ui_resource_texture")) continue;
                    IGuiTexture icon = holder.value().get();
                    menu.leaf(icon, name, () -> c.addNewResource(holder.value().get()));
                }
            }));
        }
        return container;
    }
}
