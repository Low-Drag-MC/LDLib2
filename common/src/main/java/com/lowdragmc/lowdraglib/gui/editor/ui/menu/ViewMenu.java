package com.lowdragmc.lowdraglib.gui.editor.ui.menu;

import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.gui.editor.ui.view.FloatViewWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

/**
 * @author KilaBash
 * @date 2022/12/17
 * @implNote ViewMenu
 */
@LDLRegister(name = "view", group = "editor", priority = 100)
public class ViewMenu extends MenuTab {
    public final Map<String, FloatViewWidget> openedViews = new HashMap<>();

    protected TreeBuilder.Menu createMenu() {
        var viewMenu = TreeBuilder.Menu.start().branch("ldlib.gui.editor.menu.view.window_size", menu -> {
            Minecraft minecraft = Minecraft.getInstance();
            var guiScale = minecraft.options.guiScale();
            var maxScale =  !minecraft.isRunning() ? 0x7FFFFFFE : minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
            for (int i = 0; i <= maxScale; i++) {
                var finalI = i;
                menu.leaf(guiScale.get() == i ? Icons.CHECK : IGuiTexture.EMPTY, i == 0 ? "options.guiScale.auto" : i + "", () -> {
                    if (guiScale.get() != finalI) {
                        guiScale.set(finalI);
                        Minecraft.getInstance().resizeDisplay();
                    }
                });
            }
        });
        for (AnnotationDetector.Wrapper<LDLRegister, FloatViewWidget> wrapper : AnnotationDetector.REGISTER_FLOAT_VIEWS) {
            if (editor.name().startsWith(wrapper.annotation().group())) {
                String translateKey = "ldlib.gui.editor.register.%s.%s".formatted(wrapper.annotation().group(), wrapper.annotation().name());
                String name = wrapper.annotation().name();
                if (isViewOpened(name)) {
                    viewMenu.leaf(Icons.CHECK, translateKey, () -> removeView(name));
                } else {
                    viewMenu.leaf(translateKey, () -> {
                        var view = wrapper.creator().get();
                        openView(view);
                    });
                }
            }
        }
        return viewMenu;
    }

    public void openView(FloatViewWidget view) {
        if (!isViewOpened(view.name())) {
            openedViews.put(view.name(), view);
            editor.getFloatView().addWidgetAnima(view, new Transform().duration(200).scale(0.2f));
        }
    }

    public void removeView(String viewName) {
        if (isViewOpened(viewName)) {
            for (Widget widget : editor.getFloatView().widgets) {
                if (widget instanceof FloatViewWidget view) {
                    if (view.name().equals(viewName)) {
                        editor.getFloatView().removeWidgetAnima(view, new Transform().duration(200).scale(0.2f));
                    }
                }
            }
        }
    }

    public boolean isViewOpened(String viewName) {
        if (openedViews.containsKey(viewName)) {
            for (Widget widget : editor.getFloatView().widgets) {
                if (widget instanceof FloatViewWidget view) {
                    if (view.name().equals(viewName)) {
                        return true;
                    }
                }
            }
            openedViews.remove(viewName);
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = super.serializeNBT();
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
    }
}
