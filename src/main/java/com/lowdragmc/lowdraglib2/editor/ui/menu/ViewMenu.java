package com.lowdragmc.lowdraglib2.editor.ui.menu;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ViewMenu extends MenuTab {
//    public final Map<String, FloatViewWidget> openedViews = new HashMap<>();
//    public final List<Pair<String, Supplier<FloatViewWidget>>> views = new ArrayList<>();

    public ViewMenu(Editor editor) {
        super(editor);
//        views.add(Pair.of("history_view", HistoryView::new));
    }

    @Override
    protected TreeBuilder.Menu createDefaultMenu() {
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
//        for (var view : views) {
//            String name = view.getLeft();
//            if (isViewOpened(name)) {
//                viewMenu.leaf(Icons.CHECK, name, () -> removeView(name));
//            } else {
//                viewMenu.leaf(name, () -> {
//                    openView(view.getRight().get());
//                });
//            }
//        }
        return viewMenu;
    }

    @Override
    protected Component getComponent() {
        return Component.translatable("editor.view");
    }

//    public void addView(String name, Supplier<FloatViewWidget> creator) {
//        views.add(Pair.of(name, creator));
//    }
//
//
//    public void openView(FloatViewWidget view) {
//        if (!isViewOpened(view.name())) {
//            openedViews.put(view.name(), view);
//            editor.getFloatView().addWidgetAnima(view, new Transform().duration(200).scale(0.2f));
//        }
//    }
//
//    public void removeView(String viewName) {
//        if (isViewOpened(viewName)) {
//            for (Widget widget : editor.getFloatView().widgets) {
//                if (widget instanceof FloatViewWidget view) {
//                    if (view.name().equals(viewName)) {
//                        editor.getFloatView().removeWidgetAnima(view, new Transform().duration(200).scale(0.2f));
//                    }
//                }
//            }
//        }
//    }
//
//    public boolean isViewOpened(String viewName) {
//        if (openedViews.containsKey(viewName)) {
//            for (Widget widget : editor.getFloatView().widgets) {
//                if (widget instanceof FloatViewWidget view) {
//                    if (view.name().equals(viewName)) {
//                        return true;
//                    }
//                }
//            }
//            openedViews.remove(viewName);
//        }
//        return false;
//    }

}
