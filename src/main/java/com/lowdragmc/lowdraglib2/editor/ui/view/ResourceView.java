package com.lowdragmc.lowdraglib2.editor.ui.view;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ResourceView extends View {
    public final TabView tabView = new TabView();
    public final Editor editor;
    @Getter
    private final Map<String, Resource<?>> resources = new HashMap<>();
    @Getter
    private final BiMap<String, Tab> resourceTabs= HashBiMap.create();
    @Getter @Nullable
    private Resource<?> selectedResource = null;

    public ResourceView(Editor editor) {
        super("editor.resources");
        this.editor = editor;
        this.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
            layout.setFlexDirection(YogaFlexDirection.ROW);
        });

        tabView.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW_REVERSE);
            layout.setHeightPercent(100);
            layout.setFlex(1);
        });
        tabView.tabContentContainer.layout(layout -> {
            layout.setFlex(1);
            layout.setPadding(YogaEdge.ALL, 1);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        tabView.tabHeaderContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
            layout.setHeightPercent(100);
            layout.setWidth(StyleSizeLength.AUTO);
            layout.setPadding(YogaEdge.HORIZONTAL, 1);
            layout.setPadding(YogaEdge.VERTICAL, 1);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        tabView.tabScroller
                .viewContainer(viewContainer -> viewContainer.layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.COLUMN);
                }))
                .scrollerStyle(style -> style.mode(ScrollerView.Mode.VERTICAL).verticalScrollDisplay(ScrollerView.ScrollDisplay.NEVER))
                .layout(layout -> {
                    layout.setWidth(16);
                    layout.setFlex(1);
                    layout.setMargin(YogaEdge.BOTTOM, 0);
                });
        tabView.setOnTabSelected(this::onResourceSelected);

        this.addChildren(tabView);
    }

    private void onResourceSelected(Tab tab) {
        var resource = resourceTabs.inverse().get(tab);
        if (resource != null) {
            selectedResource = getResourceByName(resource);
        }
    }

    public void addResource(Resource<?> resource) {
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
        }).style(style -> style.setTooltips(resource.getDisplayName())).addChild(new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        }).style(style -> style.backgroundTexture(resource.getIcon())));
        tabView.addTab(tab, new ResourceContainer(resource, editor));
    }

    public void addResources(Resource<?>... resources) {
        for (Resource<?> resource : resources) {
            addResource(resource);
        }
    }

    public void addResources(Resources resources) {
        for (Resource<?> resource : resources.resources.values()) {
            addResource(resource);
        }
    }

    public void removeResource(Resource<?> resource) {
        var tab = resourceTabs.remove(resource.getName());
        if (tab != null) {
            tabView.removeTab(tab);
        }
        resources.remove(resource.getName());
    }

    public void clear() {
        tabView.clear();
        resourceTabs.clear();
        resources.clear();
        selectedResource = null;
    }

    public void selectResource(Resource<?> resource) {
        selectResourceByName(resource.getName());
    }

    public void selectResourceByName(String resourceName) {
        var tab = resourceTabs.get(resourceName);
        if (tab != null) {
            tabView.selectTab(tab);
        }
    }

    /**
     * Get a resource by its name.
     */
    @Nullable
    public <T> Resource<T> getResourceByName(String resourceName) {
        return (Resource<T>) resources.get(resourceName);
    }

}
