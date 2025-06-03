package com.lowdragmc.lowdraglib.editor.ui;

import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class Window extends UIElement {
    public final TabView tabView;

    public Window() {
        this.tabView = new TabView();
        this.tabView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        }).setId("tab_view");
        getStyle().backgroundTexture(Sprites.RECT_SOLID);
        getLayout().setPadding(YogaEdge.ALL, 1);

        addEventListener(UIEvents.DRAG_ENTER, this::onDragEnter, true);
        addEventListener(UIEvents.DRAG_PERFORM, this::onDragPerform);
        addEventListener(UIEvents.DRAG_LEAVE, this::onDragLeave, true);

        tabView.tabContentContainer.layout(layout -> {
            layout.setFlex(1);
        });
        addChild(tabView);
    }

    public Window addView(View view) {
        var tab = view.craeteTab();
        tabView.addTab(tab, view);
        view._setWindowInternal(this);
        return this;
    }

    public boolean hasView(View view) {
        return tabView.getTabContents().containsValue(view);
    }

    public void removeView(View view) {
        var tab = tabView.getTabContents().inverse().get(view);
        if (tab != null) {
            tabView.removeTab(tab);
            view._setWindowInternal(null);
        }
    }

    public boolean isViewSelected(View view) {
        return view == tabView.getTabContents().get(tabView.getSelectedTab());
    }

    public void selectView(View view) {
        if (hasView(view) && !isViewSelected(view)) {
            var tab = tabView.getTabContents().inverse().get(view);
            if (tab != null) {
                tabView.selectTab(tab);
            }
        }
    }

    protected void onDragLeave(UIEvent event) {
        if (event.relatedTarget == null || !this.isAncestorOf(event.relatedTarget)) {
            style(style -> style.overlayTexture(IGuiTexture.EMPTY));
        }
    }

    protected void onDragPerform(UIEvent event) {
        style(style -> style.overlayTexture(IGuiTexture.EMPTY));
        if (event.dragHandler.draggingObject instanceof View view && !hasView(view)) {
            var oldWin = view.getWindow();
            if (oldWin != null) {
                oldWin.removeView(view);
            }
            addView(view);
            selectView(view);
        }
    }

    protected void onDragEnter(UIEvent event) {
        // check if a view is being dragged into the view
        if (event.dragHandler.draggingObject instanceof View view && !hasView(view)) {
            style(style -> style.overlayTexture(ColorPattern.T_BLUE.rectTexture()));
        }
    }
}
