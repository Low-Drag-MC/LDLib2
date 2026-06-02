package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.Nullable;
import java.io.File;

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
