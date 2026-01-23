package com.lowdragmc.lowdraglib2.editor.ui.view;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ResourceView extends View {
    public final TabView tabView = new TabView();
    public final Editor editor;
    @Getter
    private final Map<Resource<?>, ResourceInstance<?>> resources = new HashMap<>();
    @Getter
    private final BiMap<Resource<?>, Tab> resourceTabs= HashBiMap.create();
    @Getter @Nullable
    private ResourceInstance<?> selectedResourceInstance = null;

    public ResourceView(Editor editor) {
        super("editor.view.resources");
        this.editor = editor;
        getLayout().flexDirection(FlexDirection.ROW);

        tabView.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW_REVERSE);
            layout.setHeightPercent(100);
            layout.setFlex(1);
        }).moveInlineAsDefault();
        tabView.tabContentContainer.layout(layout -> {
            layout.setFlex(1);
            layout.setPadding(YogaEdge.ALL, 1);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY)).moveInlineAsDefault();
        tabView.tabHeaderContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.setHeightPercent(100);
            layout.setWidth(StyleSizeLength.AUTO);
            layout.setPadding(YogaEdge.HORIZONTAL, 1);
            layout.setPadding(YogaEdge.VERTICAL, 1);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID)).moveInlineAsDefault();
        tabView.tabScroller
                .viewContainer(viewContainer -> viewContainer.layout(layout -> {
                    layout.flexDirection(FlexDirection.COLUMN);
                }))
                .scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.NEVER))
                .layout(layout -> {
                    layout.setWidth(16);
                    layout.setFlex(1);
                    layout.setMargin(YogaEdge.BOTTOM, 0);
                }).moveInlineAsDefault();
        tabView.setOnTabSelected(this::onResourceSelected);

        this.addChildren(tabView);
    }

    private void onResourceSelected(Tab tab) {
        var resource = resourceTabs.inverse().get(tab);
        if (resource != null) {
            selectedResourceInstance = getResourceInstance(resource);
        }
    }

    public void addResourceInstance(ResourceInstance<?> resourceInstance) {
        var tab = new Tab().tabStyle(style -> {
            style.baseTexture(IGuiTexture.EMPTY);
            style.hoverTexture(Sprites.RECT_RD_T);
            style.selectedTexture(Sprites.RECT_RD_T);
        });
        tab.textStyle(style -> style.adaptiveWidth(false)).layout(layout -> {
            layout.setWidth(14);
            layout.setHeight(14);
            layout.setPadding(YogaEdge.ALL, 1);
            layout.setMargin(YogaEdge.ALL, 1);
        }).style(style -> style.tooltips(resourceInstance.resource.getDisplayName())).addChild(new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        }).style(style -> style.backgroundTexture(resourceInstance.resource.getIcon())));
        tab.moveInlineAsDefault();
        tabView.addTab(tab, new ResourceContainer<>(resourceInstance, editor));
        resources.put(resourceInstance.resource, resourceInstance);
    }

    public void addResourceInstances(ResourceInstance<?>... resources) {
        for (var resource : resources) {
            addResourceInstance(resource);
        }
    }

    public void loadResources(Resources resources) {
        resources.resources.stream().map(Resource::getResourceInstance).forEach(this::addResourceInstance);
    }

    public void removeResource(Resource<?> resource) {
        var tab = resourceTabs.remove(resource);
        if (tab != null) {
            tabView.removeTab(tab);
        }
        resources.remove(resource);
    }

    public void clear() {
        tabView.clear();
        resourceTabs.clear();
        resources.clear();
        selectedResourceInstance = null;
    }

    public void selectResourceInstance(Resource<?> resource) {
        var tab = resourceTabs.get(resource);
        if (tab != null) {
            tabView.selectTab(tab);
        }
    }

    /**
     * Get a resource by its name.
     */
    @Nullable
    public <T> ResourceInstance<T> getResourceInstance(Resource<?> resource) {
        return (ResourceInstance<T>) resources.get(resource);
    }

}
