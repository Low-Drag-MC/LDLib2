package com.lowdragmc.lowdraglib.editor_outdated.ui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.editor_outdated.ColorPattern;
import com.lowdragmc.lowdraglib.editor_outdated.ui.menu.MenuTab;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author KilaBash
 * @date 2022/12/6
 * @implNote MenuPanel
 */
public class MenuPanel extends WidgetGroup {
    public static final int HEIGHT = 16;

    @Getter
    protected final Editor editor;
    @Getter
    protected final Map<String, MenuTab> tabs = new LinkedHashMap<>();

    public MenuPanel(Editor editor) {
        super(0, 0, editor.getSize().getWidth() - ConfigPanel.WIDTH, HEIGHT);
        setClientSideWidget();
        this.editor = editor;
    }

    @Override
    public void initWidget() {
        this.setBackground(ColorPattern.T_RED.rectTexture());
        this.addWidget(new ImageWidget(2, 2, 12, 12, new ResourceTexture()));
        if (isRemote()) {
            initTabs();
        }
        super.initWidget();
    }

    public <T extends MenuTab> T getTab(String name) {
        return (T) tabs.get(name);
    }

    protected void initTabs() {
        int x = 20;
        var tag = new CompoundTag();
        try {
            tag = NbtIo.read(editor.getWorkSpace().toPath().resolve("settings/menu.cfg"));
            if (tag == null) {
                tag = new CompoundTag();
            }
        } catch (IOException e) {
            LDLib.LOGGER.error(e.getMessage());
        }
        for (var holder : LDLibRegistries.MENU_TABS) {
            if (editor.name().startsWith(holder.annotation().group())) {
                var tab = holder.value().get();
                tabs.put(holder.annotation().name(), tab);
                var button = tab.createTabWidget();
                button.addSelfPosition(x, 0);
                x += button.getSize().getWidth();
                addWidget(button);
                if (tag.contains(tab.name())) {
                    tab.deserializeNBT(tag.getCompound(tab.name()));
                }
            }
        }
    }

    public void saveMenuData() {
        var tag = new CompoundTag();
        for (MenuTab tab : tabs.values()) {
            var nbt = tab.serializeNBT();
            if (!nbt.isEmpty()) {
                tag.put(tab.name(), nbt);
            }
        }
        try {
            NbtIo.write(tag, editor.getWorkSpace().toPath().resolve("settings/menu.cfg"));
        } catch (IOException e) {
            LDLib.LOGGER.error(e.getMessage());
        }
    }

}
