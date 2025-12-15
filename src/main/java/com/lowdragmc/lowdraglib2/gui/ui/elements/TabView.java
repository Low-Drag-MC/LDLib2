package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SelectorConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.TagBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.function.Consumers;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "tab-view", group = "container", registry = "ldlib2:ui_element")
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
        this.tabHeaderContainer.addClass("__tab-view_tab_header_container__");
        this.tabScroller = new ScrollerView();
        this.tabScroller.addClass("__tab-view_tab_scroller__");
        this.tabContentContainer = new UIElement();
        this.tabContentContainer.addClass("__tab-view_tab_content_container__");

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
        internalSetup();
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
                UISoundUtils.playButtonClickSound();
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
            var content = tabContents.get(selectedTab);
            if (content != null) {
                content.setDisplay(YogaDisplay.NONE);
                content.removeClass("_tab_content_selected_");
            }
        }
        selectedTab = tab;
        selectedTab.setSelected(true);
        var content = tabContents.get(selectedTab);
        if (content != null) {
            content.setDisplay(YogaDisplay.FLEX);
            content.addClass("__tab_content_selected__");
        }
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
    public boolean canAddEditorChild(AutoRegistry.Holder<LDLRegister, UIElement, Supplier<UIElement>> holder) {
        return Tab.class.isAssignableFrom(holder.clazz());
    }

    @Override
    public void addEditorChild(UIElement child, int index) {
        if (child instanceof Tab tab) {
            addTab(tab, new UIElement(), index);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tagBuilder = TagBuilder.compound(super.serializeNBT(provider));
        tagBuilder.addList("tabs", listBuilder -> {
            for (var entry : tabContents.entrySet()) {
                var tab = entry.getKey();
                var content = entry.getValue();
                // check tab valid
                if (tab == null || !tabScroller.hasScrollViewChild(tab)) {
                    continue;
                }
                if (content == null || !tabContentContainer.hasChild(content)) {
                    continue;
                }
                // if valid, store their index for rebuild
                listBuilder.addCompound(compound -> compound
                        .add("tab", tab.getSiblingIndex())
                        .add("content", content.getSiblingIndex())
                );
            }
        });
        tagBuilder.add("selected", (selectedTab == null) ? -1 : selectedTab.getSiblingIndex());
        return tagBuilder.build();
    }

    @Override
    public void beforeDeserialize() {
        super.beforeDeserialize();
        tabContents.clear();
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag tag) {
        super.deserializeNBT(provider, tag);
        var tabs = tag.getList("tabs", Tag.TAG_COMPOUND);
        var selectedIndex = tag.getInt("selected");
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
                                UISoundUtils.playButtonClickSound();
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

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        var selectedSelector = new SelectorConfigurator<>(
                "TabView.selected",
                () -> selectedTab == null ? -1 : selectedTab.getSiblingIndex(),
                index -> {
                    if (index == -1) {
                        index = tabContents.keySet().stream().filter(Objects::nonNull)
                                .map(UIElement::getSiblingIndex)
                                .mapToInt(Integer::intValue)
                                .min().orElse(-1);
                    }
                    if (index < 0) return;
                    var finalIndex = index;
                    tabContents.keySet().stream().filter(Objects::nonNull)
                            .filter(tab -> tab.getSiblingIndex() == finalIndex)
                            .findFirst().ifPresent(this::selectTab);
                }, -1, true,
                tabContents.keySet().stream().filter(Objects::nonNull)
                        .map(UIElement::getSiblingIndex)
                        .sorted(Integer::compareTo).toList(),
                index -> index < 0 ? "auto" : Integer.toString(index));
        selectedSelector.addEventListener(UIEvents.TICK, event -> {
           var tabs = tabContents.keySet().stream().filter(Objects::nonNull)
                   .map(UIElement::getSiblingIndex)
                   .sorted(Integer::compareTo).toList();
           if (!tabs.equals(selectedSelector.selector.getCandidates())) {
               selectedSelector.selector.setCandidates(tabs);
           }
        });
        father.addConfigurator(selectedSelector);
    }
}
