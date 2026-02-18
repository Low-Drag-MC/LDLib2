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
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaGutter;

import org.jetbrains.annotations.Nullable;
import java.util.function.Supplier;

public class View extends UIElement {
    @Getter @Setter
    private String name = "view";
    @Getter @Setter
    private IGuiTexture icon = IGuiTexture.EMPTY;
    @Getter @Setter
    private boolean canRemove = false;
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
     * Get the name of the view.
     */
    protected Component getViewName() {
        return dynamicName == null ? Component.translatable(name) : dynamicName.get();
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
            }
        });
        tab.addEventListener(UIEvents.MOUSE_UP, e -> {
            lastClickTime = 0; // Reset click time
        });
        tab.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            if (lastClickTime != 0 && isMouseDown(0)) {
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

    protected void onClose() {
        Dialog.showCancelableCheck("Dialog.notify", "view.close.info", close -> {
            if (canRemove && close) {
                if (onRemove != null) {
                    onRemove.run();
                }
                removeSelf();
            }
        }, Runnables.doNothing()).show(getModularUI());
    }
}
