package com.lowdragmc.lowdraglib.editor_outdated.ui.menu;

import com.lowdragmc.lowdraglib.editor_outdated.ui.view.HistoryView;
import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.editor_outdated.ui.view.FloatViewWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/17
 * @implNote ViewMenu
 */
@LDLRegisterClient(name = "view", group = "editor", priority = 100, registry = "ldlib:menu_tab")
public class ViewMenu extends MenuTab {
    public final Map<String, FloatViewWidget> openedViews = new HashMap<>();
    public final List<Pair<String, Supplier<FloatViewWidget>>> views = new ArrayList<>();

    public ViewMenu() {
        views.add(Pair.of("history_view", HistoryView::new));
    }

    public void addView(String name, Supplier<FloatViewWidget> creator) {
        views.add(Pair.of(name, creator));
    }

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
        for (var view : views) {
            String name = view.getLeft();
            if (isViewOpened(name)) {
                viewMenu.leaf(Icons.CHECK, name, () -> removeView(name));
            } else {
                viewMenu.leaf(name, () -> {
                    openView(view.getRight().get());
                });
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
