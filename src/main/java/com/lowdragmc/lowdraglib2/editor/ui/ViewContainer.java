package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class ViewContainer extends UIElement {
    public final TabView tabView;
    public final Button collapseButton;
    public final UIElement buttonIcon;

    // runtime
    @Getter
    private boolean isCollapse;
    private final List<View> views = new ArrayList<>();
    @Nullable
    private UIElement tabPlaceHolder;
    @Nullable @Getter
    private SplittableWindow window;

    public ViewContainer() {
        this.tabView = new TabView();
        this.collapseButton = new Button().noText();
        this.addClass("__view-container__");
        this.getLayout().flex(1);

        this.tabView.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }).addClass("__view-container_tab-view__");
        getStyle().backgroundTexture(Sprites.RECT_SOLID);
        getLayout().paddingAll(1);

        tabView.tabContentContainer.layout(layout -> {
            layout.flex(1);
        });

        collapseButton.addChild(buttonIcon = new UIElement()
                .addClass("__white_icon__")
                .layout(layout -> layout.width(10).height(10))
                .style(style -> style.backgroundTexture(Icons.COLLAPSE_HORIZONTAL).tooltips("collapse_or_expand"))
        );
        collapseButton.layout(layout -> Style.defaultPipeline(layout, s -> s.width(14).height(14).alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER)));
        collapseButton.setDisplay(false);
        collapseButton.setOnClick(e -> {
            if (isCollapse) {
                expand();
            } else {
                collapse();
            }
        });
        collapseButton.addClass("__view-container_collapse-button__");


        tabView.tabScroller.getLayout().flex(1);
        tabView.tabHeaderContainer.addChildren(collapseButton);

        addChild(tabView);

        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_ENTER, this::onTabHeaderDragEnter, true);
        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_LEAVE, this::onTabHeaderDragLeave, true);
        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_UPDATE, this::onTabHeaderDragUpdate, true);
        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_PERFORM, this::onTabHeaderDragPerform);
        moveInlineAsDefault();
    }

    protected void _setWindowInternal(@Nullable SplittableWindow splittableWindow) {
        this.window = splittableWindow;
        if (this.window == null || this.window.getParentWindow() == null) {
            this.collapseButton.setDisplay(false);
            return;
        }
        this.collapseButton.setDisplay(true);
        var parentWindow = this.window.getParentWindow();
        var splitView = parentWindow.getSplitView();
        var isVertical = splitView instanceof SplitView.Vertical;
        buttonIcon.style(style -> style.backgroundTexture(isVertical ?
                Icons.COLLAPSE_VERTICAL :
                Icons.COLLAPSE_HORIZONTAL));
    }

    public void collapse() {
        if (isCollapse || this.window == null || this.window.getParentWindow() == null) return;
        this.tabView.tabScroller.setDisplay(false);
        this.tabView.tabContentContainer.setDisplay(false);
        var parentWindow = this.window.getParentWindow();
        var splitView = parentWindow.getSplitView();
        assert splitView != null;
        var isFirst = parentWindow.getFirst() == this.window;
        var isVertical = splitView instanceof SplitView.Vertical;
        this.collapseButton.layout(layout -> Style.importantPipeline(layout, s -> {
            if (isVertical) {
                s.widthPercent(100);;
            } else {
                s.heightPercent(100);
            }
        }));
        this.tabView.tabHeaderContainer.layout(layout -> Style.importantPipeline(layout,
                s -> s.paddingHorizontal(0)));
        if (!isVertical) {
            this.tabView.tabHeaderContainer.layout(layout -> Style.importantPipeline(layout,
                    s -> s.heightPercent(100)));
        }
        if (isFirst) {
            splitView.first.layout(layout -> Style.importantPipeline(layout, s -> {
                if (isVertical) {
                    s.height(16);
                } else {
                    s.width(16);
                }
            }));
        } else {
            splitView.first.layout(layout -> Style.importantPipeline(layout, s -> s.flex(1)));
            splitView.second.layout(layout -> Style.importantPipeline(layout, s -> {
                s.flexAuto();
                if (isVertical) {
                    s.height(16);
                } else {
                    s.width(16);
                }
            }));
        }
        buttonIcon.style(style -> style.backgroundTexture(isVertical ?
                Icons.EXPAND_VERTICAL :
                Icons.EXPAND_HORIZONTAL));
        isCollapse = true;
    }

    public void expand() {
        if (!isCollapse || this.window == null || this.window.getParentWindow() == null) return;
        this.tabView.tabScroller.setDisplay(true);
        this.tabView.tabContentContainer.setDisplay(true);
        var parentWindow = this.window.getParentWindow();
        var splitView = parentWindow.getSplitView();
        assert splitView != null;
        var isFirst = parentWindow.getFirst() == this.window;
        var isVertical = splitView instanceof SplitView.Vertical;
        this.collapseButton.layout(layout -> Style.importantPipeline(layout, s -> {
            if (isVertical) {
                s.set(LayoutProperties.WIDTH, null);
            } else {
                s.set(LayoutProperties.HEIGHT, null);
            }
        }));
        this.tabView.tabHeaderContainer.getStyleBag().removeCandidates(LayoutProperties.PADDING_HORIZONTAL, slot -> slot.origin() == StyleOrigin.IMPORTANT);
        if (!isVertical) {
            this.tabView.tabHeaderContainer.getStyleBag().removeCandidates(LayoutProperties.HEIGHT, slot -> slot.origin() == StyleOrigin.IMPORTANT);
        }
        if (isFirst) {
            splitView.first.getStyleBag().removeCandidates(isVertical ? LayoutProperties.HEIGHT : LayoutProperties.WIDTH, slot -> slot.origin() == StyleOrigin.IMPORTANT);
        } else {
            splitView.first.getStyleBag().removeCandidates(LayoutProperties.FLEX, slot -> slot.origin() == StyleOrigin.IMPORTANT);
            splitView.second.getStyleBag().removeCandidates(LayoutProperties.FLEX, slot -> slot.origin() == StyleOrigin.IMPORTANT);
            splitView.second.getStyleBag().removeCandidates(isVertical ? LayoutProperties.HEIGHT : LayoutProperties.WIDTH, slot -> slot.origin() == StyleOrigin.IMPORTANT);
        }
        buttonIcon.style(style -> style.backgroundTexture(isVertical ?
                Icons.COLLAPSE_VERTICAL :
                Icons.COLLAPSE_HORIZONTAL));
        isCollapse = false;

    }

    protected void onTabHeaderDragEnter(UIEvent event) {
        if (tabPlaceHolder != null) return;
        if (event.dragHandler.getDraggingObject() instanceof View) {
            tabPlaceHolder = new UIElement().layout(layout -> {
                layout.height(tabView.tabHeaderContainer.getContentHeight());
                layout.width(50);
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
        var tab = view.createTab();
        tabView.addTab(tab, view);
        views.add(view);
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
        var tab = view.createTab();
        tabView.addTab(tab, view, index);
        views.add(index, view);
        view._setWindowInternal(this);
        return this;
    }

    public ViewContainer addViews(View... views) {
        for (var view : views) {
            addView(view);
        }
        return this;
    }

    public List<View> getAllViews() {
        return views;
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
        views.remove(view);
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
