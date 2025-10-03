package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.TabBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.function.Consumers;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@LDLRegister(name = "tab_view", registry = "ldlib2:ui_element")
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
                .scrollerStyle(style -> style.mode(ScrollerMode.HORIZONTAL).horizontalScrollDisplay(ScrollDisplay.NEVER).adaptiveHeight(true))
                .layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setMargin(YogaEdge.BOTTOM, -2);
                });

        this.tabContentContainer.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 5);
        }).style(style -> style.backgroundTexture(Sprites.BORDER_THICK_RT1));

        addChildren(tabContentContainer, tabHeaderContainer);
        markAllChildrenAsInternal();
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
        tab.setTabView(this);
        if (selectedTab == null) {
            selectTab(tab);
        }
        return this;
    }

    public TabView removeTab(Tab tab) {
        if (!tabContents.containsKey(tab)) return this;
        if (tab.getTabView() == this) {
            tab.setTabView(null);
        }
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

    @Override
    public void addEditorChild(UIElement child, int index) {
        if (child instanceof Tab tab) {
            addTab(tab, new UIElement(), index);
        }
    }

    @Override
    public Tag serializeAdditionalNBT(HolderLookup.@NotNull Provider provider) {
        var tag = (CompoundTag) super.serializeAdditionalNBT(provider);
        var tabList = new ListTag();
        for (var entry : tabContents.entrySet()) {
            var tab = entry.getKey();
            var content = entry.getValue();
            // check tab valid
            if (!tabScroller.hasScrollViewChild(tab)) {
                continue;
            }
            if (!tabContentContainer.hasChild(content)) {
                continue;
            }
            // if valid, store their index for rebuild
            tabList.add(TabBuilder.compound()
                    .add("tab", tab.getSiblingIndex())
                    .add("content", content.getSiblingIndex())
                    .build());

        }
        var selectedIndex = (selectedTab == null) ? -1 : selectedTab.getSiblingIndex();
        tag.put("tabs", tabList);
        tag.putInt("selected", selectedIndex);
        return tag;
    }

    @Override
    public void beforeDeserialize() {
        super.beforeDeserialize();
        tabContents.clear();
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.@NotNull Provider provider) {
        super.deserializeAdditionalNBT(tag, provider);
        if (tag instanceof CompoundTag compoundTag) {
            var tabs = compoundTag.getList("tabs", Tag.TAG_COMPOUND);
            var selectedIndex = compoundTag.getInt("selected");
            for (var i = 0; i < tabs.size(); i++) {
                var tabCompound = tabs.getCompound(i);
                var tabIndex = tabCompound.getInt("tab");
                var contentIndex = tabCompound.getInt("content");
                if (tabIndex < tabScroller.viewContainer.getChildren().size()) {
                    var tab = tabScroller.viewContainer.getChildren().get(tabIndex);
                    if (tab instanceof Tab tabElement) {
                        if (contentIndex < tabContentContainer.getChildren().size()) {
                            var content = tabContentContainer.getChildren().get(contentIndex);
                            tabContents.put(tabElement, content);
                            tabElement.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                                if (event.button == 0) {
                                    Widget.playButtonClickSound();
                                    selectTab(tabElement);
                                }
                            });
                            content.setDisplay(YogaDisplay.NONE);
                            if (selectedIndex == i) {
                                selectTab(tabElement);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
    }
}
