package com.lowdragmc.lowdraglib.editor.ui.resource;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib.editor.ui.Editor;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.Dialog;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.appliedenergistics.yoga.*;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.*;

@Accessors(chain = true)
public class ResourceProviderContainer<T> extends UIElement {
    public final ScrollerView scrollerView = new ScrollerView();
    public final IResourceProvider<T> resourceProvider;
    private final Map<IResourcePath, UIElement> resourceUIs = new HashMap<>();
    @Setter
    protected Function<IResourcePath, UIElement> uiSupplier = path -> new UIElement().layout(layout -> {
        layout.setWidthPercent(100);
        layout.setHeightPercent(100);
    }).style(style -> style.backgroundTexture(Icons.FILE));
    protected Function<IResourcePath, String> nameSupplier;
    @Setter
    protected Predicate<IResourcePath> canRemove;
    @Setter
    protected Predicate<IResourcePath> canRename;
    @Setter
    protected Predicate<IResourcePath> canEdit;
    @Setter
    protected Predicate<IResourcePath> canCopy;
    @Setter
    protected BooleanSupplier supportAdd;
    @Setter
    @Nullable
    protected BiConsumer<ResourceProviderContainer<T>, IResourcePath> onEdit = null;
    @Setter
    @Nullable
    protected Supplier<T> addDefault = null;
    @Setter
    @Nullable
    protected BiConsumer<ResourceProviderContainer<T>, TreeBuilder.Menu> onMenu;

    // runtime
    @Getter
    protected HashSet<IResourcePath> dirtyResources = new HashSet<>();
    @Getter @Nullable
    protected IResourcePath selected = null;
    @Getter @Setter
    protected Editor editor;

    public ResourceProviderContainer(IResourceProvider<T> resourceProvider) {
        getLayout().setWidthPercent(100);
        getLayout().setFlex(1);
        this.resourceProvider = resourceProvider;
        this.nameSupplier = resourceProvider::getResourceName;
        this.canRemove = resourceProvider::canRemove;
        this.canRename = resourceProvider::canRename;
        this.canEdit = resourceProvider::canEdit;
        this.canCopy = resourceProvider::canCopy;
        this.supportAdd = resourceProvider::supportAdd;

        this.scrollerView.scrollerStyle(style -> {
            style.mode(ScrollerView.Mode.VERTICAL).verticalScrollDisplay(ScrollerView.ScrollDisplay.ALWAYS);
        }).layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        });
        this.scrollerView.viewContainer.layout(layout -> {
           layout.setFlexDirection(YogaFlexDirection.ROW);
           layout.setWrap(YogaWrap.WRAP);
        });
        addChild(scrollerView);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button == 1 && editor != null) {
            editor.openMenu(event.x, event.y, getMenu());
        }
    }

    protected UIElement createResourceUI(IResourcePath key) {
        return new UIElement().layout(layout -> {
            if (resourceProvider.getResourceHolder().isList()) {
                layout.setWidthPercent(100);
                layout.setFlexDirection(YogaFlexDirection.ROW);
                layout.setMargin(YogaEdge.VERTICAL, 1);
            } else {
                layout.setWidth(resourceProvider.getResourceHolder().getUiWidth());
                layout.setFlexDirection(YogaFlexDirection.COLUMN);
                layout.setMargin(YogaEdge.ALL, 3);
            }
            layout.setGap(YogaGutter.ALL, 2);
        }).addChildren(new UIElement().layout(layout -> {
            if (resourceProvider.getResourceHolder().isList()) {
                layout.setWidth(resourceProvider.getResourceHolder().getUiWidth());
            } else {
                layout.setWidthPercent(100);
            }
            layout.setAspectRatio(1);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setJustifyContent(YogaJustify.CENTER);
        }).addChild(uiSupplier.apply(key)), new Label().textStyle(style -> {
            if (resourceProvider.getResourceHolder().isList()) {
                style.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL);
            } else {
                style.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL);
            }
        }).setText(nameSupplier.apply(key)).setOverflow(YogaOverflow.HIDDEN).layout(layout -> {
            if (resourceProvider.getResourceHolder().isList()) {
                layout.setFlex(1);
                layout.setHeightPercent(100);
                layout.setJustifyContent(YogaJustify.CENTER);
            } else {
                layout.setHeight(14);
            }
        })).addEventListener(UIEvents.MOUSE_DOWN, e -> selectResource(key))
                .addEventListener(UIEvents.DOUBLE_CLICK, e-> editResource(key));
    }

    /**
     * Call this method to reload the resource container.
     */
    public void reloadResourceContainer() {
        resourceUIs.clear();
        scrollerView.clearAllScrollViewChildren();
        resourceProvider.forEach(entry -> appendResourceUI(entry.getKey()));
    }

    /**
     * Reloads a specific resource UI by its path.
     * If the resource does not exist, it will not do anything.
     * @param path the resource path to reload
     */
    public void reloadSpecificResource(IResourcePath path) {
        if (path == null || !resourceUIs.containsKey(path) || !resourceProvider.hasResource(path)) return;
        var ui = createResourceUI(path);
        var index = scrollerView.viewContainer.getChildren().indexOf(resourceUIs.get(path));
        scrollerView.removeScrollViewChild(resourceUIs.get(path));
        scrollerView.addScrollViewChildAt(ui, index);
        resourceUIs.put(path, ui);
        if (selected != null && selected.equals(path)) {
            selectResource(path);
        }
    }

    public void appendResourceUI(IResourcePath resourcePath) {
        if (resourcePath == null || resourceUIs.containsKey(resourcePath) || !resourceProvider.hasResource(resourcePath)) return;
        var ui = createResourceUI(resourcePath);
        resourceUIs.put(resourcePath, ui);
        scrollerView.addScrollViewChild(ui);
    }

    public void selectResource(IResourcePath resourcePath) {
        if (!resourceUIs.containsKey(resourcePath)) {
            resourcePath = null;
        }
        if (selected != null) {
            var previousUI = resourceUIs.get(selected);
            if (previousUI != null) {
                previousUI.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
            }
        }
        selected = resourcePath;
        if (selected != null) {
            var selectedUI = resourceUIs.get(selected);
            if (selectedUI != null) {
                selectedUI.style(style -> style.overlayTexture(ColorPattern.T_DARK_GRAY.rectTexture()));
            }
        }
    }

    public void setUiWidth(int uiWidth) {
        if (resourceProvider.getResourceHolder().getUiWidth() != uiWidth && uiWidth > 0) {
            resourceProvider.getResourceHolder().setUiWidth(uiWidth);
            if (resourceProvider.getResourceHolder().isList()) {
                reloadResourceContainer();
            } else {
                for (UIElement element : resourceUIs.values()) {
                    element.layout(layout -> layout.setWidth(uiWidth));
                }
            }
        }
    }

    /**
     * Marks a resource as dirty, which means it will be reloaded on the next tick.
     */
    public void markResourceDirty(IResourcePath resourcePath) {
        if (resourcePath != null && resourceProvider.hasResource(resourcePath)) {
            dirtyResources.add(resourcePath);
        }
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (resourceProvider.tickResourceProvider()) {
            reloadResourceContainer();
            dirtyResources.clear();
        }
        // check for dirty resources and reload them
        for (var dirtyResource : dirtyResources) {
            var resource = resourceProvider.getResource(dirtyResource);
            if (resource == null) continue;
            resourceProvider.addResource(dirtyResource, resource);
            reloadSpecificResource(dirtyResource);
        }
        dirtyResources.clear();
    }

    public void setList(boolean isList) {
        if (resourceProvider.getResourceHolder().isList() != isList) {
            resourceProvider.getResourceHolder().setList(isList);
            reloadResourceContainer();
        }
    }

    protected TreeBuilder.Menu getMenu() {
        var menu = TreeBuilder.Menu.start();
        var isList = resourceProvider.getResourceHolder().isList();
        var uiWidth = resourceProvider.getResourceHolder().getUiWidth();
        menu.leaf(isList ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.list", () -> setList(!isList));
        menu.branch("ldlib.gui.editor.group.size", m -> {
            m.leaf(uiWidth == 15 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.small", () -> setUiWidth(15));
            m.leaf(uiWidth == 30 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.medium", () -> setUiWidth(30));
            m.leaf(uiWidth == 50 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.large", () -> setUiWidth(50));
            m.leaf(uiWidth == 100 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.extra_large", () -> setUiWidth(100));
        });
        menu.crossLine();
        if (selected != null && canEdit.test(selected) && onEdit != null) {
            menu.leaf(Icons.EDIT_FILE, "ldlib.gui.editor.menu.edit", () -> editResource(selected));
        }
        if (selected != null && canRename.test(selected)) {
            menu.leaf("ldlib.gui.editor.menu.rename", () -> renameResource(selected));
        }
        menu.crossLine();
        if (selected != null && canCopy.test(selected)) {
            menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> copyResource(selected));
        }
        if (supportAdd.getAsBoolean() && addDefault != null) {
            menu.leaf(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", this::addNewResource);
        }
        if (selected != null && canRemove.test(selected)) {
            menu.leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", () -> removeResource(selected, true));
        }
        resourceProvider.onMenu(menu);
        if (onMenu != null) {
            onMenu.accept(this, menu);
        }
        return menu;
    }

    protected void addNewResource() {
        if (supportAdd.getAsBoolean() && addDefault != null) {
            var newResource = addDefault.get();
            if (newResource != null) {
                addNewResource(newResource);
            }
        }
    }

    public void addNewResource(T value) {
        if (value == null) return;
        var key = resourceProvider.createPath("new resource");
        var count = 1;
        while (resourceProvider.hasResource(key)) {
            key = resourceProvider.createPath("new resource (" + count + ")");
            count++;
        }
        resourceProvider.addResource(key, value);
        appendResourceUI(key);
        selectResource(key);
    }

    public void copyResource(IResourcePath key) {
        if (key != null && canCopy.test(key)) {
            var value = resourceProvider.getResource(key);
            if (value != null) {
                var tag = resourceProvider.getResourceHolder().serialize(value, Platform.getFrozenRegistry());
                if (tag != null) {
                    var copied = resourceProvider.getResourceHolder().deserialize(tag, Platform.getFrozenRegistry());
                    if (copied != null) {
                        var count = 1;
                        var newKey = resourceProvider.createPath(resourceProvider.getResourceName(key) + " copy");
                        while(resourceProvider.hasResource(newKey)) {
                            newKey = resourceProvider.createPath(resourceProvider.getResourceName(key) + " copy (" + count + ")");
                            count++;
                        }
                        resourceProvider.addResource(newKey, copied);
                        appendResourceUI(newKey);
                    }
                }
            }
        }
    }

    public void removeResource(IResourcePath key, boolean confirm) {
        if (key != null && canRemove.test(key)) {
            if (confirm) {
                Dialog.showCheckBox("ldlib.gui.editor.menu.remove", "editor.remove.confirm", result -> {
                    if (result) {
                        resourceProvider.removeResource(key);
                        var ui = resourceUIs.remove(key);
                        if (ui != null) {
                            scrollerView.removeScrollViewChild(ui);
                        }
                        if (selected == key) {
                            selected = null;
                        }
                    }
                }).show(editor);
            } else {
                resourceProvider.removeResource(key);
                var ui = resourceUIs.remove(key);
                if (ui != null) {
                    scrollerView.removeScrollViewChild(ui);
                }
                if (selected == key) {
                    selected = null;
                }
            }
        }
    }

    public void editResource(IResourcePath key) {
        if (key != null && canEdit.test(key) && onEdit != null) {
            onEdit.accept(this, key);
        }
    }

    public void renameResource(IResourcePath key) {
        if (key != null && canRename.test(key)) {
            var ui = resourceUIs.get(key);
            if (ui != null && ui.getChildren().getLast() instanceof Label label) {
                // remove current label and add a TextField for renaming
                var textField = new TextField().setText(nameSupplier.apply(key));
                textField.addEventListener(UIEvents.BLUR, e -> {
                    var newName = textField.getText().trim();
                    var newPath = resourceProvider.createPath(newName);
                    if (newPath.equals(key)) {
                        // if the name is the same, just update the label
                        label.setText(nameSupplier.apply(key));
                        label.removeChild(textField);
                        return;
                    }
                    // find a unique name
                    var count = 0;
                    while (resourceProvider.hasResource(newPath)) {
                        count++;
                        newPath = resourceProvider.createPath(newName + " (" + count + ")");
                    }
                    resourceProvider.addResource(newPath, resourceProvider.getResource(key));
                    resourceProvider.removeResource(key);
                    removeResource(key, false);
                    appendResourceUI(newPath);
                    selectResource(newPath);
                });
                textField.addEventListener(UIEvents.KEY_DOWN, e -> {
                    if (e.keyCode == GLFW.GLFW_KEY_ENTER) {
                        textField.blur();
                    } else if (e.keyCode == GLFW.GLFW_KEY_ESCAPE) {
                        textField.setText(nameSupplier.apply(key), false);
                        textField.blur();
                    }
                });
                label.setText("");
                label.addChild(textField);
                textField.focus();
            }
        }
    }

}
