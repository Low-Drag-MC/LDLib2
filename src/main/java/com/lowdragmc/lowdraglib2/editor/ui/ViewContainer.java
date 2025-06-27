package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class ViewContainer extends UIElement {
    public final TabView tabView;

    // runtime
    @Nullable
    private UIElement tabPlaceHolder;

    public ViewContainer() {
        this.tabView = new TabView();
        this.tabView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        }).setId("tab_view");
        getStyle().backgroundTexture(Sprites.RECT_SOLID);
        getLayout().setPadding(YogaEdge.ALL, 1);

        tabView.tabContentContainer.layout(layout -> {
            layout.setFlex(1);
        });
        addChild(tabView);

        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_ENTER, this::onTabHeaderDragEnter, true);
        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_LEAVE, this::onTabHeaderDragLeave, true);
        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_UPDATE, this::onTabHeaderDragUpdate, true);
        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_PERFORM, this::onTabHeaderDragPerform);
    }

    protected void onTabHeaderDragEnter(UIEvent event) {
        if (tabPlaceHolder != null) return;
        if (event.dragHandler.getDraggingObject() instanceof View) {
            tabPlaceHolder = new UIElement().layout(layout -> {
                layout.setHeight(tabView.tabHeaderContainer.getSizeHeight());
                layout.setWidth(50);
            }).style(style -> style.backgroundTexture(ColorPattern.GRAY.rectTexture()));
            var index = -1;
            for (var tab : tabView.tabScroller.viewContainer.getChildren()) {
                if (tab.isMouseOverElement(event.x, event.y)) {
                    index = tabView.tabScroller.viewContainer.getChildren().indexOf(tab);
                    if (tab.getSizeWidth() / 2 + tab.getPositionX() < event.x) {
                        index++;
                    }
                }
            }
            if (index == -1) {
                tabView.tabScroller.addScrollViewChild(tabPlaceHolder);
            } else {
                tabView.tabScroller.addScrollViewChildAt(tabPlaceHolder, index);
            }
        }
    }

    protected void onTabHeaderDragLeave(UIEvent event) {
        if (tabView.tabHeaderContainer.isMouseOverElement(event.x, event.y)) return;
        if (tabPlaceHolder != null) {
            tabPlaceHolder.removeSelf();
            tabPlaceHolder = null;
        }
    }

    protected void onTabHeaderDragUpdate(UIEvent event) {
        if (tabPlaceHolder == null) return;
        if (event.dragHandler.getDraggingObject() instanceof View) {
            var index = -1;
            var placeHolderIndex = tabView.tabScroller.viewContainer.getChildren().indexOf(tabPlaceHolder);
            for (var tab : tabView.tabScroller.viewContainer.getChildren()) {
                if (tab.isMouseOverElement(event.x, event.y)) {
                    if (tab == tabPlaceHolder) return;
                    index = tabView.tabScroller.viewContainer.getChildren().indexOf(tab);
                    if (tab.getSizeWidth() / 2 + tab.getPositionX() < event.x) {
                        index++;
                    }
                }
            }
            tabPlaceHolder.removeSelf();
            if (index == -1) {
                tabView.tabScroller.addScrollViewChild(tabPlaceHolder);
            } else {
                if (index > placeHolderIndex) {
                    index--;
                }
                tabView.tabScroller.addScrollViewChildAt(tabPlaceHolder, index);
            }
        }
    }

    protected void onTabHeaderDragPerform(UIEvent event) {
        if (tabPlaceHolder == null) return;
        if (event.dragHandler.getDraggingObject() instanceof View view) {
            var index = tabView.tabScroller.viewContainer.getChildren().indexOf(tabPlaceHolder);
            tabPlaceHolder.removeSelf();
            tabPlaceHolder = null;
            if (view.getViewContainer() != null && view.getViewContainer() == this) {
                var tab = tabView.getTabContents().inverse().get(view);
                if (tab != null && tab.getParent() != null && tab.getParent().getChildren().indexOf(tab) < index) {
                    index--;
                }
            }
            if (index != -1) {
                addViewAt(view, index);
                selectView(view);
            }
        }
    }

    /**
     * Adds the specified view to the ViewContainer, setting up its associated tab and updating
     * its internal state to reference this container.
     *
     * @param view the {@link View} instance to add to the container. This view will be
     *             removed from its current container, have a tab created for it, and
     *             its internal state updated.
     */
    public ViewContainer addView(View view) {
        view.removeSelf();
        var tab = view.craeteTab();
        tabView.addTab(tab, view);
        view._setWindowInternal(this);
        return this;
    }

    /**
     * Adds the specified view to the ViewContainer at the specified index, setting up its associated tab
     * and updating its internal state to reference this container.
     *
     * @param view the {@link View} instance to be added. This view will be removed from its current container,
     *             have a tab created for it, and its internal state updated to reference this container.
     * @param index the position at which the view should be inserted in the container. If the index is out of bounds,
     *              it may be adjusted to fit within the valid range.
     */
    public ViewContainer addViewAt(View view, int index) {
        view.removeSelf();
        var tab = view.craeteTab();
        tabView.addTab(tab, view, index);
        view._setWindowInternal(this);
        return this;
    }

    public ViewContainer addViews(View... views) {
        for (var view : views) {
            addView(view);
        }
        return this;
    }

    public boolean hasView(View view) {
        return tabView.getTabContents().containsValue(view);
    }

    public boolean isEmptyWindow() {
        return tabView.getTabContents().isEmpty();
    }

    /**
     * Removes the specified view from the ViewContainer. This operation will also remove
     * the associated tab from the tab view and update the internal lifecycle state of the view.
     * If the ViewContainer becomes empty as a result of this operation and the parent of
     * the ViewContainer is a splittable window, it triggers the parent window's empty state handling.
     *
     * @param view the {@link View} instance to be removed. The method will unlink the view
     *             from this container and remove its tab representation. If the container
     *             becomes empty, window-specific cleanup actions may be executed.
     */
    public void removeView(View view) {
        var tab = tabView.getTabContents().inverse().get(view);
        if (tab != null) {
            tabView.removeTab(tab);
            view._setWindowInternal(null);
        }
        if (isEmptyWindow() && getParent() instanceof SplittableWindow splittableWindow) {
            splittableWindow.onWindowsEmpty();
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
}
