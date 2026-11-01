package com.lowdragmc.lowdraglib2.editor.ui.floating;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorLayout;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.OffscreenSurface;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import com.lowdragmc.lowdraglib2.gui.ui.window.WindowBounds;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

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
    /**
     * Where each view's window was last left, by view name.
     *
     * <p>Keyed by name rather than by identity so it survives the editor being closed and rebuilt,
     * which is the same key {@link FloatingLayout} and {@link #findDockedView} already use.
     */
    private final Map<String, WindowBounds> rememberedBounds = new LinkedHashMap<>();
    /** Each view's window's gui scale setting, by view name, kept the same way as {@link #rememberedBounds}. */
    private final Map<String, Integer> rememberedScales = new LinkedHashMap<>();

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
            window.setGuiScale(initialScale(view));
            var bounds = initialBounds(view);
            if (window.open(bounds.x(), bounds.y(), bounds.width(), bounds.height(), false)) {
                attach(window);
                window.dock(view);
                return true;
            }
        }
        return openFallbackPanel(view, title);
    }

    /**
     * Where {@code view}'s window should open.
     *
     * <p>Where it was last time, if it has been floated before. Otherwise over the pane it is being
     * torn out of, at the same size and nudged clear of it — so the window appears to come off the
     * spot the user clicked rather than materialising somewhere unrelated at a stock size. The nudge
     * grows with the number of windows already open, so tearing out two tabs of the same pane does
     * not stack them exactly.
     */
    private WindowBounds initialBounds(View view) {
        var remembered = rememberedBounds.get(view.getName());
        if (remembered != null) return remembered.atLeastMinimum();

        var pane = paneBounds(view);
        if (pane == null) {
            // Nothing laid out to measure — a view floated before the editor has drawn a frame. Let
            // the platform place it.
            return new WindowBounds(Integer.MIN_VALUE, Integer.MIN_VALUE, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
        var nudge = CASCADE_STEP * (windows.size() + 1);
        return pane.offsetBy(nudge, nudge).atLeastMinimum();
    }

    /**
     * What {@code view}'s window was last set to, or else whatever the window it is torn out of draws
     * at, so it comes off at the size it was.
     */
    private int initialScale(View view) {
        var remembered = rememberedScales.get(view.getName());
        if (remembered != null) return remembered;
        var source = view.getModularUI() == null ? null : ModularUIWindow.windowOf(view.getModularUI());
        return source == null ? OffscreenSurface.FOLLOW_GAME : source.getGuiScaleSetting();
    }

    /**
     * The rectangle {@code view} currently occupies on the desktop, or {@code null} if it has no size
     * to measure.
     *
     * <p>Three coordinate spaces meet here. The pane's rectangle is in the element tree's local
     * space, {@code getWorldMouse} lifts it into the GUI space of the window it is shown in,
     * multiplying by that window's GUI scale gives its pixels, and its desktop position turns those
     * into the virtual-screen pixels a window is positioned in. Skipping any one of them puts the
     * window somewhere plausible-looking and wrong.
     *
     * <p>The container rather than the view: the view is the tab's contents, and what the user sees
     * themselves tearing out is the pane around it.
     */
    @Nullable
    private WindowBounds paneBounds(View view) {
        UIElement pane = view.getViewContainer() != null ? view.getViewContainer() : view;
        if (pane.getSizeWidth() <= 0 || pane.getSizeHeight() <= 0) return null;
        var topLeft = pane.getWorldMouse(pane.getPositionX(), pane.getPositionY());
        var bottomRight = pane.getWorldMouse(pane.getPositionX() + pane.getSizeWidth(),
                pane.getPositionY() + pane.getSizeHeight());

        var surface = UISurface.of(pane.getModularUI());
        var scale = surface.guiScale();
        var originX = new int[1];
        var originY = new int[1];
        GLFW.glfwGetWindowPos(surface.windowHandle(), originX, originY);
        return new WindowBounds(
                originX[0] + (int) (topLeft.x * scale),
                originY[0] + (int) (topLeft.y * scale),
                (int) ((bottomRight.x - topLeft.x) * scale),
                (int) ((bottomRight.y - topLeft.y) * scale));
    }

    /**
     * Records where {@code window} is and its gui scale, against every view it currently holds.
     *
     * <p>Called on the way out rather than every frame: a view leaves a floating window by being
     * docked back or by the window closing, and both of those come through here.
     */
    private void rememberPlacement(FloatingViewWindow window) {
        var bounds = window.restoredBounds();
        for (var view : window.views()) {
            if (bounds != null) {
                rememberedBounds.put(view.getName(), bounds);
            }
            rememberedScales.put(view.getName(), window.getGuiScaleSetting());
        }
    }

    /**
     * Sets {@code window}'s gui scale, see {@link ModularUIWindow#setGuiScale}, and remembers it for
     * the views in it.
     */
    public void setWindowScale(FloatingViewWindow window, int setting) {
        window.setGuiScale(setting);
        for (var view : window.views()) {
            rememberedScales.put(view.getName(), window.getGuiScaleSetting());
        }
    }

    /**
     * Adds a submenu for picking {@code window}'s gui scale: follow the game, auto, or any fixed scale
     * the window's size allows. Used by the tab menu of a view in it and by its title bar.
     */
    public void appendScaleMenu(TreeBuilder.Menu menu, FloatingViewWindow window) {
        menu.branch("editor.window.scale", scales -> {
            var current = window.getGuiScaleSetting();
            scales.leaf(current == OffscreenSurface.FOLLOW_GAME ? Icons.CHECK : IGuiTexture.EMPTY,
                    "editor.window.scale.follow_game", () -> setWindowScale(window, OffscreenSurface.FOLLOW_GAME));
            scales.leaf(current == 0 ? Icons.CHECK : IGuiTexture.EMPTY,
                    "options.guiScale.auto", () -> setWindowScale(window, 0));
            for (int scale = 1; scale <= Math.max(window.getMaxGuiScale(), current); scale++) {
                var picked = scale;
                scales.leaf(current == picked ? Icons.CHECK : IGuiTexture.EMPTY,
                        Integer.toString(picked), () -> setWindowScale(window, picked));
            }
        });
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
            // Before it leaves: once it is docked back the window no longer lists it, and a window
            // with other tabs left in it will not pass through closeWindow at all.
            rememberPlacement(window);
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
        rememberPlacement(window);
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
                            EditorLayout.captureSlots(window.rootWindow())),
                    window.getGuiScaleSetting()));
        }
        return layouts;
    }

    /**
     * Every remembered rectangle, including the windows still open right now.
     *
     * <p>Saved alongside the layout so that floating a view, docking it back and quitting does not
     * throw the arrangement away. Without it the memory would be oddly half-persistent: a window
     * left open when the editor closes is restored by {@link #capture()}, but one docked back an
     * hour earlier would open at a stock size next session.
     */
    public Map<String, WindowBounds> captureBounds() {
        for (var window : windows) {
            rememberPlacement(window);
        }
        return Map.copyOf(rememberedBounds);
    }

    /** Every remembered gui scale setting, including the windows still open right now. */
    public Map<String, Integer> captureScales() {
        for (var window : windows) {
            rememberPlacement(window);
        }
        return Map.copyOf(rememberedScales);
    }

    /** Seeds the remembered gui scale settings from a saved layout. */
    public void restoreScales(Map<String, Integer> scales) {
        rememberedScales.putAll(scales);
    }

    /** Seeds the remembered rectangles from a saved layout. */
    public void restoreBounds(Map<String, WindowBounds> bounds) {
        rememberedBounds.putAll(bounds);
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
            window.setGuiScale(layout.guiScale());
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
