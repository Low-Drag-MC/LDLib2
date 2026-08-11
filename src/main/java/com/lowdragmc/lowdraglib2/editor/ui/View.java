package com.lowdragmc.lowdraglib2.editor.ui;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class View extends UIElement {
    @Getter @Setter
    private String name = "view";
    @Getter @Setter
    private IGuiTexture icon = IGuiTexture.EMPTY;
    @Getter @Setter
    private boolean canRemove = false;
    /**
     * Whether this view may be torn out of the editor into a window of its own.
     *
     * <p>Turn it off for a view that either
     * <ul>
     *   <li>renders against the game window's own dimensions rather than the surface it is drawn
     *       into — a 3D scene view unprojects mouse positions against the main window and restores
     *       its viewport from it, so it would end up interactive in the wrong place; or</li>
     *   <li>binds synced values or RPC events. Floating moves the view into another
     *       {@code ModularUI}, and those bindings re-register against that UI's own sync manager,
     *       which has no server counterpart.</li>
     * </ul>
     */
    @Getter @Setter
    private boolean floatable = true;
    private long lastClickTime = 0;

    @Nullable
    @Setter
    private Runnable onRemove;
    // runtime
    @Setter
    @Nullable
    protected Supplier<Component> dynamicName;
    @Getter
    @Nullable
    private ViewContainer viewContainer;

    public View() {
        getLayout().widthPercent(100);
        getLayout().heightPercent(100);
    }

    public View(String name) {
        this();
        this.name = name;
    }

    public View(String name, IGuiTexture icon) {
        this();
        this.name = name;
        this.icon = icon;
    }

    @Override
    public boolean removeSelf() {
        if (viewContainer != null) {
            viewContainer.removeView(this);
            return true;
        } else {
            return super.removeSelf();
        }
    }

    /**
     * Set the window for this view. This is used internally to manage the view's lifecycle and interactions.
     */
    protected void _setWindowInternal(ViewContainer viewContainer) {
        this.viewContainer = viewContainer;
    }

    /**
     * The split-tree window this view is docked in, or {@code null} while it is undocked.
     */
    @Nullable
    public SplittableWindow getWindow() {
        return viewContainer == null ? null : viewContainer.getWindow();
    }

    /**
     * The editor this view belongs to, or {@code null} when it is not attached to one.
     *
     * <p>Found by walking up to the nearest {@link EditorHost}, not by looking for an {@link Editor}
     * directly: a floated view lives in a different element tree entirely, rooted in its own window
     * rather than in the editor.
     */
    @Nullable
    public Editor getEditor() {
        for (UIElement element = getParent(); element != null; element = element.getParent()) {
            if (element instanceof EditorHost host) {
                return host.getHostedEditor();
            }
        }
        return null;
    }

    /**
     * Get the name of the view.
     */
    protected Component getViewName() {
        return dynamicName == null ? Component.translatable(name) : dynamicName.get();
    }

    /**
     * The display name, for anything outside this class that needs to label the view — a window
     * title, say.
     */
    public Component getViewNameComponent() {
        return getViewName();
    }

    /**
     * Create a tab for this view which will be displayed in the window's tab view.
     */
    public Tab createTab() {
        var tab = new Tab();
        if (dynamicName == null) {
            tab.setText(getViewName());
        } else {
            tab.setDynamicText(this::getViewName);
        }
        if (icon != IGuiTexture.EMPTY && icon != null) {
            tab.getLayout().gapAll(2);
            tab.addChildAt(new UIElement().layout(layout -> {
                layout.heightPercent(100);
                layout.setAspectRatio(1f);
            }).style(style -> style.backgroundTexture(icon)), 0);
        }
        if (canRemove) {
            tab.addChild(new Button().setOnClick(e -> {
                if (e.button == 0) {
                    onClose();
                    // prevent drag event from propagating
                    e.stopPropagation();
                }
            }).noText().buttonStyle(buttonStyle -> buttonStyle.baseTexture(Icons.CLOSE)
                    .hoverTexture(Icons.CLOSE.copy().setColor(ColorPattern.LIGHT_GRAY.color))
                    .pressedTexture(Icons.CLOSE.copy().setColor(ColorPattern.GRAY.color))).layout(layout -> {
                layout.heightPercent(100);
                layout.setAspectRatio(1f);
            }));
        }
        tab.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) {
                lastClickTime = System.currentTimeMillis();
            } else if (e.button == 1) {
                var editor = getEditor();
                if (editor != null) {
                    editor.openMenu(tab, e.x, e.y, createTabMenu());
                }
            }
        });
        tab.addEventListener(UIEvents.MOUSE_UP, e -> {
            lastClickTime = 0; // Reset click time
        });
        tab.addEventListener(UIEvents.DOUBLE_CLICK, e -> {
            if (e.button == 0) {
                var window = getWindow();
                if (window != null) {
                    window.toggleMaximize();
                }
            }
        });
        tab.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            if (lastClickTime != 0 && isMouseDown(0)) {
                // Dropping a tab into a pane you cannot see is meaningless, so a drag ends the
                // focus mode before it starts.
                var window = getWindow();
                if (window != null) {
                    window.restoreMaximized();
                }
                var w = tab.getSizeWidth();
                var h = tab.getSizeHeight();
                tab.startDrag(this, new GuiTextureGroup(ColorPattern.T_WHITE.rectTexture(), new TextTexture(name).setWidth((int) w)))
                        .setDragTexture(- w / 2, -h / 2, w, h);
                tab.setDisplay(false);
            }
            lastClickTime = 0;
        }, true);
        tab.addEventListener(UIEvents.DRAG_END, e -> {
            tab.setDisplay(true);
        });
        return tab;
    }

    /**
     * The tab's right-click menu. Override to add entries; call {@code super} first to keep the
     * standard ones.
     */
    protected TreeBuilder.Menu createTabMenu() {
        var menu = TreeBuilder.Menu.start();
        var window = getWindow();
        // A root window already fills its tree, so there is nothing to maximize it into.
        if (window != null && window.getParentWindow() != null) {
            var maximized = window.isMaximized();
            menu.leaf(maximized ? Icons.WINDOW_RESTORE : Icons.WINDOW_MAXIMIZE,
                    maximized ? "editor.tab.restore" : "editor.tab.maximize",
                    window::toggleMaximize);
        }
        var editor = getEditor();
        if (editor != null && floatable) {
            var floating = editor.getFloatingViews();
            if (floating.isFloating(this)) {
                menu.leaf(Icons.WINDOW_RESTORE, "editor.tab.dock_back", () -> floating.dockBack(this));
            } else {
                menu.leaf(Icons.WINDOW_MAXIMIZE, "editor.tab.float", () -> floating.floatView(this));
            }
        }
        if (canRemove) {
            menu.crossLine();
            menu.leaf(Icons.CLOSE, "editor.tab.close", this::onClose);
        }
        var hasClosableSiblings = hasClosableSiblings();
        if (hasClosableSiblings) {
            menu.leaf("editor.tab.close_others", () -> closeBatch(view -> view != this));
        }
        if (canRemove || hasClosableSiblings) {
            menu.leaf("editor.tab.close_all", () -> closeBatch(view -> true));
        }
        return menu;
    }

    private boolean hasClosableSiblings() {
        return viewContainer != null
                && viewContainer.getAllViews().stream().anyMatch(view -> view != this && view.canRemove);
    }

    /**
     * Close every closable view in this pane matching {@code filter}, behind a single confirmation
     * rather than one per view.
     */
    private void closeBatch(Predicate<View> filter) {
        if (viewContainer == null) return;
        // Copy: closing mutates the container's live view list.
        var targets = viewContainer.getAllViews().stream()
                .filter(view -> view.canRemove)
                .filter(filter)
                .toList();
        if (targets.isEmpty()) return;
        Dialog.showCancelableCheck("Dialog.notify", "view.close.info", close -> {
            if (close) {
                targets.forEach(View::closeImmediately);
            }
        }, Runnables.doNothing()).show(getModularUI());
    }

    protected void onClose() {
        Dialog.showCancelableCheck("Dialog.notify", "view.close.info", close -> {
            if (close) {
                closeImmediately();
            }
        }, Runnables.doNothing()).show(getModularUI());
    }

    /**
     * Remove this view without asking. {@link #onClose()} is the prompting entry point.
     */
    protected void closeImmediately() {
        if (!canRemove) return;
        if (onRemove != null) {
            onRemove.run();
        }
        removeSelf();
    }
}
