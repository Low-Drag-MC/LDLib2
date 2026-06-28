package com.lowdragmc.lowdraglib2.editor.resource;

import com.google.common.collect.Maps;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.editor.view.UIEditorView;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class UIResourceProviderContainer extends ResourceProviderContainer<UITemplate> {
    private final Map<UUID, Tuple<IResourcePath, UIEditorView>> openedViews = Maps.newHashMap();

    public UIResourceProviderContainer(IResourceProvider<UITemplate> provider) {
        super(provider);
        setAddDefault(() -> UITemplate.of(new UIElement().layout(layout -> {
            layout.width(150);
            layout.height(150);
        }).addClass("panel_bg"), StylesheetManager.GDP)
        ).setUiSupplier(path -> new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }).style(style -> style.backgroundTexture(Icons.WIDGET_BASIC)))
        .setOnEdit((container, path) -> {
            // if there is an existing view open, don't open a new one'
            if (openedViews.values().stream().map(Tuple::getA).anyMatch(path::equals)) return;

            var template = provider.getResource(path);
            if (template == null) return;
            var editor = container.getEditor();
            var uuid = UUID.randomUUID();

            var newView = new UIEditorView().loadTemplate(template, newTemplate -> {
                if (!openedViews.containsKey(uuid)) {
                    // invalid already.
                    return;
                }
                var realPath = openedViews.get(uuid).getA();
                provider.addResource(realPath, newTemplate);
                container.reloadSpecificResource(realPath);
            });
            // cache path for renaming cases
            AtomicReference<IResourcePath> pathCache = new AtomicReference<>(path);
            newView.addEventListener(UIEvents.ADDED, e -> {
                openedViews.put(uuid, new Tuple<>(pathCache.get(), newView));
            });
            newView.addEventListener(UIEvents.REMOVED, e -> {
                var pair = openedViews.remove(uuid);
                if (pair != null) {
                    pathCache.set(pair.getA());
                }
            });
            newView.setCanRemove(true);
            newView.setIcon(Icons.WIDGET_BASIC);
            newView.setDynamicName(() -> {
                if (openedViews.containsKey(uuid)) {
                    return Component.literal(openedViews.get(uuid).getA().getResourceName());
                } else {
                    return Component.literal(pathCache.get().getResourceName());
                }
            });
            editor.placeView(newView, () -> editor.centerWindow.getLeftTop());
        });
    }

    @Override
    protected void onRename(IResourcePath oldPath, IResourcePath newPath) {
        super.onRename(oldPath, newPath);
        // update open view name as well
        for (var openedView : openedViews.values()) {
            if (openedView.getA().equals(oldPath)) {
                openedView.setA(newPath);
            }
        }
    }
}
