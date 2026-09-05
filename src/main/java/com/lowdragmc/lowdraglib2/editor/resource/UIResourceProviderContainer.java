package com.lowdragmc.lowdraglib2.editor.resource;

import com.google.common.collect.Maps;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.editor.view.UIEditorView;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class UIResourceProviderContainer extends ResourceProviderContainer<UITemplate> {
    private final Map<UUID, Pair<IResourcePath, UIEditorView>> openedViews = Maps.newHashMap();

    public UIResourceProviderContainer(IResourceProvider<UITemplate> provider) {
        super(provider);
        // A built-in cannot be edited, but it is exactly the thing worth looking at: it is how a mod
        // shows the way one of its UIs is put together, and the starting point for someone who wants
        // their own. Opening is therefore always allowed; whether the view writes back is decided
        // below, from the same canEdit answer.
        setCanOpen(path -> true);
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
            if (openedViews.values().stream().map(Pair::left).anyMatch(path::equals)) return;

            var template = provider.getResource(path);
            if (template == null) return;
            var editor = container.getEditor();
            var uuid = UUID.randomUUID();
            var editable = provider.canEdit(path);

            // A null save handler is what makes the view read-only: UIEditorView only writes back
            // when it has one, so a built-in can be opened and read without any way to change it.
            var newView = new UIEditorView().loadTemplate(template, !editable ? null : newTemplate -> {
                if (!openedViews.containsKey(uuid)) {
                    // invalid already.
                    return;
                }
                var realPath = openedViews.get(uuid).left();
                provider.addResource(realPath, newTemplate);
                container.reloadSpecificResource(realPath);
            });
            // cache path for renaming cases
            AtomicReference<IResourcePath> pathCache = new AtomicReference<>(path);
            newView.addEventListener(UIEvents.ADDED, e -> {
                openedViews.put(uuid, Pair.of(pathCache.get(), newView));
            });
            newView.addEventListener(UIEvents.REMOVED, e -> {
                var pair = openedViews.remove(uuid);
                if (pair != null) {
                    pathCache.set(pair.left());
                }
            });
            newView.setCanRemove(true);
            newView.setIcon(Icons.WIDGET_BASIC);
            newView.setDynamicName(() -> {
                var name = openedViews.containsKey(uuid)
                        ? openedViews.get(uuid).left().getResourceName()
                        : pathCache.get().getResourceName();
                // Said in the tab rather than left to be discovered: a view that silently discards
                // edits is worse than one that will not take them.
                return Component.literal(editable ? name : name + " (read-only)");
            });
            editor.placeView(newView, () -> editor.centerWindow.getLeftTop());
        });
    }

    @Override
    protected void onRename(IResourcePath oldPath, IResourcePath newPath) {
        super.onRename(oldPath, newPath);
        // update open view name as well
        for (var entry : openedViews.entrySet()) {
            var openedView = entry.getValue();

            if (openedView.left().equals(oldPath)) {
                entry.setValue(
                        Pair.of(newPath, openedView.right())
                );
            }
        }
    }
}
