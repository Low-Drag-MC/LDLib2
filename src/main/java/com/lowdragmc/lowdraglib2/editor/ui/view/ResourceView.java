package com.lowdragmc.lowdraglib2.editor.ui.view;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.browser.AssetBrowser;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
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
    /**
     * The file system view of the {@code ldlib2} folder. It is pinned in front of the per resource type
     * tabs and survives project loading, so it is not part of {@link #resourceTabs}.
     */
    @Getter
    private final AssetBrowser assetBrowser;
    @Getter
    private final Tab assetBrowserTab;

    public ResourceView(Editor editor) {
        super("editor.view.resources");
        this.editor = editor;
        getLayout().flexDirection(FlexDirection.ROW);

        tabView.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW_REVERSE);
            layout.heightPercent(100);
            layout.flex(1);
        }).moveInlineAsDefault();
        tabView.tabContentContainer.layout(layout -> {
            layout.flex(1);
            layout.paddingAll(1);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY)).moveInlineAsDefault();
        tabView.tabHeaderContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.heightPercent(100);
            layout.widthAuto();
            layout.paddingHorizontal(1);
            layout.paddingVertical(1);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID)).moveInlineAsDefault();
        tabView.tabScroller
                .viewContainer(viewContainer -> viewContainer.layout(layout -> {
                    layout.flexDirection(FlexDirection.COLUMN);
                }))
                .scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.NEVER))
                .layout(layout -> {
                    layout.width(16);
                    layout.flex(1);
                    layout.marginBottom(0);
                }).moveInlineAsDefault();
        tabView.setOnTabSelected(this::onResourceSelected);

        this.addChildren(tabView);

        this.assetBrowser = new AssetBrowser(editor);
        this.assetBrowserTab = createTab(Icons.FOLDER, Component.translatable("editor.view.assets"));
        tabView.addTab(assetBrowserTab, assetBrowser, 0);
    }

    private void onResourceSelected(Tab tab) {
        // the asset browser is not tied to a single resource type
        var resource = tab == assetBrowserTab ? null : resourceTabs.inverse().get(tab);
        selectedResourceInstance = resource == null ? null : getResourceInstance(resource);
    }

    private Tab createTab(IGuiTexture icon, Component tooltip) {
        var tab = new Tab().tabStyle(style -> {
            style.baseTexture(IGuiTexture.EMPTY);
            style.hoverTexture(Sprites.RECT_RD_T);
            style.selectedTexture(Sprites.RECT_RD_T);
        });
        tab.textStyle(style -> style.adaptiveWidth(false)).layout(layout -> {
            layout.width(14);
            layout.height(14);
            layout.paddingAll(1);
            layout.marginAll(1);
        }).style(style -> style.tooltips(tooltip)).addChild(new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }).style(style -> style.backgroundTexture(icon)));
        tab.moveInlineAsDefault();
        return tab;
    }

    public void addResourceInstance(ResourceInstance<?> resourceInstance) {
        var resource = resourceInstance.resource;
        var previous = resourceTabs.remove(resource);
        if (previous != null) {
            tabView.removeTab(previous);
        }
        var tab = createTab(resource.getIcon(), resource.getDisplayName());
        // registered before addTab: adding the first tab selects it right away, which calls back into
        // onResourceSelected and needs the tab to already be resolvable.
        resources.put(resource, resourceInstance);
        resourceTabs.put(resource, tab);
        tabView.addTab(tab, new ResourceContainer<>(resourceInstance, editor));
    }

    public void addResourceInstances(ResourceInstance<?>... resources) {
        for (var resource : resources) {
            addResourceInstance(resource);
        }
    }

    public void loadResources(Resources resources) {
        // the selected tab is deliberately left alone: loading a project keeps whatever was showing,
        // which for a fresh editor and after clear() is the pinned asset browser
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
        // the resource tabs are removed one by one instead of clearing the whole tab view, so the
        // pinned asset browser tab stays in place (and keeps its selection state consistent).
        for (var tab : List.copyOf(resourceTabs.values())) {
            tabView.removeTab(tab);
        }
        resourceTabs.clear();
        resources.clear();
        selectedResourceInstance = null;
        tabView.selectTab(assetBrowserTab);
        assetBrowser.reset();
    }

    @Override
    public void screenTick() {
        super.screenTick();
        // the browser's borrowed resource behaviors have to keep flushing dirty resources to disk even
        // while another tab is showing, and hidden children are not ticked by the framework.
        assetBrowser.tickBehaviors();
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
