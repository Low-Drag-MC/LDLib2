package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;

public abstract class Resource<T> {
    public enum DisplayMode {
        LIST,
        GRID,
    }
    @Getter @Setter
    private DisplayMode defaultDisplayMode = DisplayMode.GRID;
    @Getter @Setter
    private int defaultUIWidth = 30;
    @Getter(lazy = true)
    private final ResourceInstance<T> resourceInstance = createResourceInstance();

    public Resource() {
    }

    /**
     * Resource icon, it can be used to display the resource in the UI.
     */
    public abstract IGuiTexture getIcon();

    /**
     * Resource name, it can also be used to obtain the resource from the resource view.
     */
    public abstract String getName();

    /**
     * The file extension for this resource type, used for {@link FileResourceProvider}
     */
    public String getFileExtension() {
        return "." + getName() + ".nbt";
    }

    protected ResourceInstance<T> createResourceInstance() {
        return new ResourceInstance<>(this);
    }

    /**
     * Generate builtin resources
     */
    public void buildBuiltin(ResourceInstance<T> resourceInstance) {
        var builtinProvider = new BuiltinResourceProvider<>("built-in", resourceInstance);
        buildBuiltin(builtinProvider);
        resourceInstance.addBuiltinProvider(builtinProvider);

        var global = new FileResourceProvider<>(resourceInstance, new File(LDLib2.getAssetsDir(), "ldlib2/resources/global"));
        global.setName("global");
        resourceInstance.addBuiltinProvider(global);

        // send an Event to register built resources
        var event = new EditorResourceEvent.LoadBuiltin(resourceInstance);
        ModLoader.postEvent(event);
    }

    /**
     * Generate builtin resources
     */
    public void buildBuiltin(BuiltinResourceProvider<T> provider) {
    }

    /**
     * Create a resource provider container for the given provider. You should override it to attach additional UI elements or behaviors.
     * e.g. how to add a new resource, how to display the resource in the UI, etc.
     */
    public ResourceProviderContainer<T> createResourceProviderContainer(IResourceProvider<T> provider) {
        return new ResourceProviderContainer<>(provider);
    }

    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    /**
     * Whether a file dropped onto the editor from outside the game can be turned into a resource of
     * this type.
     * <p>
     * Every resource type reads back the files it writes itself, so the default accepts anything with
     * this type's {@link #getFileExtension()}. Override to take other formats in as well — an image for
     * a texture, a model json for a renderer — and remember to keep the {@code super} call unless the
     * intent really is to stop accepting this type's own files.
     */
    public boolean canImportFile(File file) {
        return file.isFile() && file.getName().endsWith(getFileExtension());
    }

    /**
     * Builds a resource from a dropped file and reports it back through the context. Only called for
     * files {@link #canImportFile} accepted.
     * <p>
     * The import may finish later than this method returns, so that it can ask the player where an
     * external file should be saved first. See {@link ResourceImportContext}.
     */
    public void importFile(ResourceImportContext<T> context) {
        var file = context.getFile();
        if (!file.getName().endsWith(getFileExtension())) {
            context.cancel();
            return;
        }
        // one of our own resource files: read the value out of it and let the provider write its own copy
        try {
            var tag = NbtIo.read(file.toPath());
            if (tag != null && tag.getString("type").equals(getName())) {
                var value = deserializeResource(tag.get("data"), Platform.getFrozenRegistry());
                if (value != null) {
                    var name = file.getName();
                    context.complete(name.substring(0, name.length() - getFileExtension().length()), value);
                    return;
                }
            }
            context.fail("editor.resource.import_unreadable");
        } catch (IOException e) {
            LDLib2.LOGGER.error("Failed to import resource file {}: ", file, e);
            context.fail("editor.resource.import_unreadable");
        }
    }

    /**
     * Serialize resource to nbt for persistence.
     */
    @Nullable
    public abstract Tag serializeResource(T value, HolderLookup.Provider provider);

    /**
     * Deserialize resource from nbt.
     */
    @Nullable
    public abstract T deserializeResource(Tag nbt, HolderLookup.Provider provider);

    @Override
    public String toString() {
        return getName();
    }

}
