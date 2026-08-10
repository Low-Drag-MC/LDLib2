package com.lowdragmc.lowdraglib2.editor.ui.floating;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorLayout;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.ViewContainer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns an {@link Editor}'s torn-off views: which ones are floating, in what, and how to get them
 * back.
 *
 * <p>Floating prefers a real operating-system window. When one cannot be opened — an old driver, a
 * platform without windowing, Minecraft sitting in a macOS fullscreen Space where a second window
 * would land somewhere the user cannot see it — the view goes into a draggable in-game panel
 * instead. The menu entry always does something; only how it looks changes.
 */
@OnlyIn(Dist.CLIENT)
public class FloatingViewManager {

    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 400;
    /**
     * Offset applied to each successive window so a second float does not land exactly on the first.
     */
    private static final int CASCADE_STEP = 28;

    private final Editor editor;
    private final List<FloatingViewWindow> windows = new ArrayList<>();
    /**
     * Views hosted in the in-game fallback panel, and the panel hosting them.
     */
    private final Map<View, Dialog> panels = new LinkedHashMap<>();

    public FloatingViewManager(Editor editor) {
        this.editor = editor;
    }

    /**
     * Tears {@code view} out of the editor into a window of its own.
     *
     * @return {@code false} if the view refused to float, in which case it has not been moved
     */
    public boolean floatView(View view) {
        if (!view.isFloatable() || isFloating(view)) return false;
        var title = view.getViewNameComponent();
        if (OsWindowManager.isAvailable()) {
            var window = new FloatingViewWindow(editor, title);
            var offset = CASCADE_STEP * windows.size();
            if (window.open(Integer.MIN_VALUE, Integer.MIN_VALUE, DEFAULT_WIDTH, DEFAULT_HEIGHT, false)) {
                if (offset > 0) {
                    var os = window.window();
                    os.setPosition(os.getPositionX() + offset, os.getPositionY() + offset);
                }
                attach(window);
                window.dock(view);
                return true;
            }
        }
        return openFallbackPanel(view, title);
    }

    private void attach(FloatingViewWindow window) {
        windows.add(window);
        window.setOnCloseRequested(() -> closeWindow(window));
    }

    /**
     * The in-game panel used when no native window is available. Reuses {@link Dialog}'s window mode,
     * which is already draggable and resizable.
     */
    private boolean openFallbackPanel(View view, Component title) {
        var mui = editor.getModularUI();
        if (mui == null) return false;
        LDLib2.LOGGER.info("[floating-view] no native window available, hosting '{}' in an in-game panel",
                view.getName());
        var origin = view.getViewContainer();
        var dialog = new Dialog();
        dialog.setTitle(title.getString());
        dialog.setAutoClose(false);
        dialog.allowInteraction();
        dialog.windowMode(editor.getPositionX() + 40, editor.getPositionY() + 40, DEFAULT_WIDTH / 2f, DEFAULT_HEIGHT / 2f);
        dialog.setClickOutsideClose(false);
        if (origin != null) {
            origin.removeView(view);
        }
        dialog.addContent(view);
        dialog.setOnClose(() -> {
            panels.remove(view);
            editor.redockView(view);
        });
        panels.put(view, dialog);
        dialog.show(mui);
        return true;
    }

    public boolean isFloating(View view) {
        return panels.containsKey(view) || windowOf(view) != null;
    }

    /**
     * Puts {@code view} back where the editor would have placed it.
     */
    public void dockBack(View view) {
        var panel = panels.remove(view);
        if (panel != null) {
            // Detach before closing, or the dialog takes the view down with it.
            view.removeSelf();
            panel.setOnClose(null);
            panel.close();
            editor.redockView(view);
            return;
        }
        for (var window : List.copyOf(windows)) {
            if (!window.views().contains(view)) continue;
            editor.redockView(view);
            if (window.isEmpty()) {
                closeWindow(window);
            }
            return;
        }
    }

    /**
     * Closes {@code window}, returning whatever is still in it to the editor.
     */
    public void closeWindow(FloatingViewWindow window) {
        if (!windows.remove(window)) return;
        for (var view : window.views()) {
            editor.redockView(view);
        }
        window.close();
    }

    public void closeAll() {
        for (var window : List.copyOf(windows)) {
            closeWindow(window);
        }
        for (var view : List.copyOf(panels.keySet())) {
            dockBack(view);
        }
    }

    /**
     * Every view currently living outside the editor's own dock tree.
     */
    public List<View> allViews() {
        var views = new ArrayList<View>(panels.keySet());
        for (var window : windows) {
            views.addAll(window.views());
        }
        return views;
    }

    @Nullable
    public FloatingViewWindow windowOf(View view) {
        for (var window : windows) {
            if (window.views().contains(view)) return window;
        }
        return null;
    }

    // ------------------------------------------------------------------------------- persistence

    /**
     * Snapshots the native windows. The fallback panels are deliberately not saved: they exist only
     * because a native window could not be opened, and that may not be true next session.
     */
    public List<FloatingLayout> capture() {
        var layouts = new ArrayList<FloatingLayout>();
        for (var window : windows) {
            if (window.isEmpty() || !window.isOpen()) continue;
            var os = window.window();
            layouts.add(new FloatingLayout(os.getPositionX(), os.getPositionY(),
                    os.getWindowWidth(), os.getWindowHeight(),
                    new EditorLayout(window.rootWindow().getLayoutConfig(),
                            EditorLayout.captureSlots(window.rootWindow()))));
        }
        return layouts;
    }

    /**
     * Re-opens saved windows, taking the named views out of the editor.
     *
     * <p>Views that no longer exist are skipped, and a window left with nothing in it is not opened
     * at all — a renamed or removed view should not leave an empty window on the user's desktop.
     */
    public void restore(List<FloatingLayout> layouts) {
        for (var layout : layouts) {
            var views = new ArrayList<View>();
            for (var name : layout.viewNames()) {
                var view = findDockedView(name);
                if (view != null && view.isFloatable()) {
                    views.add(view);
                }
            }
            if (views.isEmpty()) continue;

            var title = views.get(0).getViewNameComponent();
            if (!OsWindowManager.isAvailable()) {
                for (var view : views) {
                    openFallbackPanel(view, view.getViewNameComponent());
                }
                continue;
            }
            var window = new FloatingViewWindow(editor, title);
            var width = layout.width() > 0 ? layout.width() : DEFAULT_WIDTH;
            var height = layout.height() > 0 ? layout.height() : DEFAULT_HEIGHT;
            if (!window.open(layout.x(), layout.y(), width, height, false)) {
                for (var view : views) {
                    openFallbackPanel(view, view.getViewNameComponent());
                }
                continue;
            }
            attach(window);
            for (var view : views) {
                window.dock(view);
            }
        }
    }

    @Nullable
    private View findDockedView(String name) {
        for (var view : editor.rootWindow.getAllViews()) {
            if (view.getName().equals(name)) return view;
        }
        return null;
    }

}
