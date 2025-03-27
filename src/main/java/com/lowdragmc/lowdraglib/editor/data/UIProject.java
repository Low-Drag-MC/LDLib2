package com.lowdragmc.lowdraglib.editor.data;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.editor.ui.Editor;
import com.lowdragmc.lowdraglib.editor.ui.MainPanel;
import com.lowdragmc.lowdraglib.editor.ui.tool.WidgetToolBox;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
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
@LDLRegister(name = "ui", group = "editor.ui")
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
        return new UIProject(Resources.defaultResource(),
                (WidgetGroup) new WidgetGroup(30, 30, 200, 200).setBackground(ResourceBorderTexture.BORDERED_BACKGROUND));
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("resources", resources.serializeNBT(provider));
        tag.put("root", IConfigurableWidget.serializeNBT(this.root, resources, true, provider));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.resources = loadResources(tag.getCompound("resources"));
        this.root = new WidgetGroup();
        IConfigurableWidget.deserializeNBT(this.root, tag.getCompound("root"), resources, true, provider);
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
            IConfigurableWidget.deserializeNBT(root, data, resources, false, provider);
            return root;
        };
    }

    @Nullable
    public static Supplier<WidgetGroup> loadUIFromFile(ResourceLocation location) {
        try {
            var file = new File(LDLib.getLDLibDir(), "assets/%s/projects/ui/%s.ui".formatted(location.getNamespace(), location.getPath()));
            return loadUIFromTag(Platform.getFrozenRegistry(), NbtIo.read(file.toPath()));
        } catch (Exception e) {
            LDLib.LOGGER.error("Failed to load ui project from file: {}", location, e);
            return null;
        }
    }
}
