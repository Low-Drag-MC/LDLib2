package com.lowdragmc.lowdraglib.gui.editor.data;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.editor.ui.MainPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.tool.WidgetToolBox;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nullable;
import java.io.DataInputStream;
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
        deserializeNBT(tag);
    }

    public UIProject newEmptyProject() {
        return new UIProject(Resources.defaultResource(),
                (WidgetGroup) new WidgetGroup(30, 30, 200, 200).setBackground(ResourceBorderTexture.BORDERED_BACKGROUND));
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("resources", resources.serializeNBT());
        tag.put("root", IConfigurableWidget.serializeNBT(this.root, resources, true));
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.resources = loadResources(tag.getCompound("resources"));
        this.root = new WidgetGroup();
        IConfigurableWidget.deserializeNBT(this.root, tag.getCompound("root"), resources, true);
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
    public static Supplier<WidgetGroup> loadUIFromTag(CompoundTag tag) {
        var resources = Resources.fromNBT(tag.getCompound("resources"));
        var data = tag.getCompound("root");
        return () -> {
            var root = new WidgetGroup();
            IConfigurableWidget.deserializeNBT(root, data, resources, false);
            return root;
        };
    }

    @Nullable
    public static Supplier<WidgetGroup> loadUIFromFile(ResourceLocation location) {
        try {
            var file = new File(LDLib.getLDLibDir(), "assets/%s/projects/ui/%s.ui".formatted(location.getNamespace(), location.getPath()));
            return loadUIFromTag(NbtIo.read(file));
        } catch (Exception e) {
            LDLib.LOGGER.error("Failed to load ui project from file: {}", location, e);
            return null;
        }
    }
}
