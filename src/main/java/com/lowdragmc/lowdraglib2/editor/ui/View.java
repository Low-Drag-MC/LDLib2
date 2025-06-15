package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaGutter;

import javax.annotation.Nullable;

public class View extends UIElement {
    @Getter @Setter
    private String name = "view";
    @Getter @Setter
    private IGuiTexture icon = IGuiTexture.EMPTY;
    private long lastClickTime = 0;
    // runtime
    @Getter
    @Nullable
    private Window window;

    public View() {}

    public View(String name) {
        this.name = name;
    }

    public View(String name, IGuiTexture icon) {
        this.name = name;
        this.icon = icon;
    }

    /**
     * Set the window for this view. This is used internally to manage the view's lifecycle and interactions.
     */
    protected void _setWindowInternal(Window window) {
        this.window = window;
    }

    /**
     * Get the name of the view.
     */
    protected Component getViewName() {
        return Component.translatable(name);
    }

    /**
     * Create a tab for this view which will be displayed in the window's tab view.
     */
    public Tab craeteTab() {
        var tab = new Tab().setText(getViewName());
        if (icon != IGuiTexture.EMPTY && icon != null) {
            tab.getLayout().setGap(YogaGutter.ALL, 2);
            tab.addChildAt(new UIElement().layout(layout -> {
                layout.setHeightPercent(100);
                layout.setAspectRatio(1f);
            }).style(style -> style.backgroundTexture(icon)), 0);
        }
        tab.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) {
                lastClickTime = System.currentTimeMillis();
            }
        });
        tab.addEventListener(UIEvents.MOUSE_UP, e -> {
            lastClickTime = 0; // Reset click time
        });
        tab.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            if (lastClickTime != 0 && isMouseDown(0)) {
                tab.startDrag(this, new TextTexture(name));
            }
            lastClickTime = 0;
        }, true);
        return tab;
    }

}
