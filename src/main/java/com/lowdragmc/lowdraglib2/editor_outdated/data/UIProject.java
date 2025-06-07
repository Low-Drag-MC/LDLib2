package com.lowdragmc.lowdraglib2.editor_outdated.data;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor_outdated.data.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor_outdated.data.resource.EntriesResource;
import com.lowdragmc.lowdraglib2.editor_outdated.data.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.editor_outdated.ui.Editor;
import com.lowdragmc.lowdraglib2.editor_outdated.ui.MainPanel;
import com.lowdragmc.lowdraglib2.editor_outdated.ui.tool.WidgetToolBox;
import com.lowdragmc.lowdraglib2.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/4
 * @implNote UIProject
 */
@LDLRegisterClient(name = "ui", group = "editor.ui", registry = "ldlib2:project")
public class UIProject implements IProject {

    public Resources resources;
    public WidgetGroup root;

    private UIProject() {

    }

    public UIProject(Resources resources, WidgetGroup root) {
        this.resources = resources;
        this.root = root;
    }

    public UIProject(CompoundTag tag) {
        deserializeNBT(Platform.getFrozenRegistry(), tag);
    }

    public UIProject(CompoundTag tag, HolderLookup.Provider provider) {
        deserializeNBT(provider, tag);
    }

    public UIProject newEmptyProject() {
        return new UIProject(Resources.ofDefault(new EntriesResource(), new ColorsResource(), new TexturesResource()),
                (WidgetGroup) new WidgetGroup(30, 30, 200, 200).setBackground(ResourceBorderTexture.BORDERED_BACKGROUND));
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("resources", resources.serializeNBT(provider));
        tag.put("root", this.root.serializeNBT(provider));
        // todo resource
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.resources = loadResources(tag.getCompound("resources"));
        this.root = new WidgetGroup();
        this.root.deserializeNBT(provider, tag.getCompound("root"));
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    public void onLoad(Editor editor) {
        IProject.super.onLoad(editor);
        editor.getTabPages().addTab("Main", new MainPanel(editor, root));

        for (WidgetToolBox.Default tab : WidgetToolBox.Default.TABS) {
            editor.getToolPanel().addNewToolBox("ldlib.gui.editor.group." + tab.groupName, tab.icon, tab::createToolBox);
        }
    }

    /**
     * Load ui project file for productive environment.
     * @return an ui creator which caches the resources to speed up the creation process.
     */
    public static Supplier<WidgetGroup> loadUIFromTag(HolderLookup.Provider provider, CompoundTag tag) {
        var resources = Resources.fromNBT(tag.getCompound("resources"));
        var data = tag.getCompound("root");
        return () -> {
            var root = new WidgetGroup();
            root.deserializeNBT(provider, data);
            // todo resource
            return root;
        };
    }

    @Nullable
    public static Supplier<WidgetGroup> loadUIFromFile(ResourceLocation location) {
        try {
            var file = new File(LDLib2.getAssetsDir(), "%s/projects/ui/%s.ui".formatted(location.getNamespace(), location.getPath()));
            return loadUIFromTag(Platform.getFrozenRegistry(), NbtIo.read(file.toPath()));
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to load ui project from file: {}", location, e);
            return null;
        }
    }
}
