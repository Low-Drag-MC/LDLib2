package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.apache.commons.lang3.function.Consumers;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class TabView extends UIElement {
    public final UIElement tabHeaderContainer;
    public final ScrollerView tabScroller;
    public final UIElement tabContentContainer;
    @Getter
    private final BiMap<Tab, UIElement> tabContents = HashBiMap.create();
    @Setter
    private Consumer<Tab> onTabSelected = Consumers.nop();
    // runtime
    @Nullable
    @Getter
    private Tab selectedTab = null;

    public TabView() {
        getLayout().setFlexDirection(YogaFlexDirection.COLUMN_REVERSE);

        this.tabHeaderContainer = new UIElement().setId("tab_header");
        this.tabScroller = new ScrollerView();
        this.tabScroller.setId("tab_scroller");
        this.tabContentContainer = new UIElement().setId("tab_container");

        this.tabHeaderContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setPadding(YogaEdge.HORIZONTAL, 3);
            layout.setWidthPercent(100);
        }).addChild(tabScroller);

        this.tabScroller.viewPort(viewPort -> viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY)).layout(layout -> layout.setPadding(YogaEdge.ALL, 0)))
                .viewContainer(viewContainer -> viewContainer.layout(layout -> layout.setFlexDirection(YogaFlexDirection.ROW)))
                .scrollerStyle(style -> style.mode(ScrollerView.Mode.HORIZONTAL).horizontalScrollDisplay(ScrollerView.ScrollDisplay.NEVER).adaptiveHeight(true))
                .layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setMargin(YogaEdge.BOTTOM, -2);
                });

        this.tabContentContainer.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 5);
        }).style(style -> style.backgroundTexture(Sprites.BORDER_THICK_RT1));

        addChildren(tabContentContainer, tabHeaderContainer);
    }

    public TabView addTab(Tab tab, UIElement content) {
        return addTab(tab, content, -1);
    }

    public TabView addTab(Tab tab, UIElement content, int index) {
        if (index < 0) {
            index = tabContents.size() + 1 + index;
        }
        tab.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                Widget.playButtonClickSound();
                selectTab(tab);
            }
        });
        content.setDisplay(YogaDisplay.NONE);
        tabScroller.addScrollViewChildAt(tab, index);
        tabContentContainer.addChildAt(content, index);
        tabContents.put(tab, content);
        if (selectedTab == null) {
            selectTab(tab);
        }
        return this;
    }

    public TabView removeTab(Tab tab) {
        var content = tabContents.remove(tab);
        if (content != null) {
            tabScroller.removeScrollViewChild(tab);
            tabContentContainer.removeChild(content);
        }
        tab.setSelected(false);
        if (selectedTab == tab) {
            selectedTab = null;
            var newTab = tabContents.keySet().stream().findFirst().orElse(null);
            if (newTab != null) {
                selectTab(newTab);
            }
        }
        return this;
    }

    public TabView clear() {
        tabContents.clear();
        tabScroller.clearAllScrollViewChildren();
        tabContentContainer.clearAllChildren();
        return this;
    }

    public TabView selectTab(Tab tab) {
        if (tab == selectedTab) {
            return this;
        }
        if (selectedTab != null) {
            selectedTab.setSelected(false);
            if (tabContents.containsKey(selectedTab)) {
                tabContents.get(selectedTab).setDisplay(YogaDisplay.NONE);
            }
        }
        selectedTab = tab;
        selectedTab.setSelected(true);
        tabContents.get(selectedTab).setDisplay(YogaDisplay.FLEX);
        onTabSelected.accept(selectedTab);
        return this;
    }

    public TabView tabHeaderContainer(Consumer<UIElement> style) {
        style.accept(tabHeaderContainer);
        return this;
    }

    public TabView tabScroller(Consumer<ScrollerView> style) {
        style.accept(tabScroller);
        return this;
    }

    public TabView tabContentContainer(Consumer<UIElement> style) {
        style.accept(tabContentContainer);
        return this;
    }
}
