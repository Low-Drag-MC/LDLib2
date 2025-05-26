package com.lowdragmc.lowdraglib.editor.ui.resource;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib.editor.resource.ResourceProvider;
import com.lowdragmc.lowdraglib.editor.ui.Editor;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
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
import net.minecraft.Util;
import org.appliedenergistics.yoga.*;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.*;

@Accessors(chain = true)
public class ResourceProviderContainer<T> extends UIElement {
    public final ScrollerView scrollerView = new ScrollerView();
    public final ResourceProvider<T> resourceProvider;
    private final Map<IResourcePath, UIElement> resourceUIs = new HashMap<>();
    @Setter
    protected Function<IResourcePath, UIElement> uiSupplier = path -> new UIElement().layout(layout -> {
        layout.setWidthPercent(100);
        layout.setHeightPercent(100);
    }).style(style -> style.backgroundTexture(Icons.FILE));
    protected Function<IResourcePath, String> nameSupplier = IResourcePath::getResourceName;
    @Setter
    protected Predicate<IResourcePath> canRemove;
    @Setter
    protected Predicate<IResourcePath> canRename;
    @Setter
    protected Predicate<IResourcePath> canEdit;
    @Setter
    protected Predicate<IResourcePath> canCopy;
    @Setter
    protected BiConsumer<ResourceProviderContainer<T>, IResourcePath> onEdit = (c, k) -> {};
    @Setter
    @Nullable
    protected Supplier<T> addDefault = null;
    @Setter
    @Nullable
    protected BiConsumer<ResourceProviderContainer<T>, TreeBuilder.Menu> onMenu;

    // runtime
    protected int uiWidth = 30;
    @Getter @Nullable
    protected IResourcePath selected = null;
    @Getter @Setter
    protected Editor editor;

    public ResourceProviderContainer(ResourceProvider<T> resourceProvider) {
        getLayout().setWidthPercent(100);
        getLayout().setFlex(1);
        this.resourceProvider = resourceProvider;
        this.canRemove = resourceProvider::canRemove;
        this.canRename = resourceProvider::canRename;
        this.canEdit = resourceProvider::canEdit;
        this.canCopy = resourceProvider::canCopy;

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
            layout.setWidth(uiWidth);
            layout.setMargin(YogaEdge.ALL, 3);
            layout.setGap(YogaGutter.ALL, 2);
        }).addChildren(new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setAspectRatio(1);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setJustifyContent(YogaJustify.CENTER);
        }).addChild(uiSupplier.apply(key)), new Label().textStyle(style -> style
                        .textAlignHorizontal(Horizontal.CENTER)
                        .textAlignVertical(Vertical.CENTER)
                        .textWrap(TextWrap.HOVER_ROLL))
                .setText(nameSupplier.apply(key)).setOverflow(YogaOverflow.HIDDEN)
                        .layout(layout -> layout.setHeight(14)))
                .addEventListener(UIEvents.MOUSE_DOWN, e -> selectResource(key))
                .addEventListener(UIEvents.DOUBLE_CLICK, e-> editResource(key));
    }

    /**
     * Call this method to reload the resource container.
     */
    public void reloadResourceContainer() {
        resourceUIs.clear();
        scrollerView.clearAllScrollViewChildren();
        resourceProvider.forEach(entry -> {
            var key = entry.getKey();
            var ui = createResourceUI(key);
            resourceUIs.put(key, ui);
            scrollerView.addScrollViewChild(ui);
        });
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
        if (this.uiWidth != uiWidth && uiWidth > 0) {
            this.uiWidth = uiWidth;
            for (UIElement element : resourceUIs.values()) {
                element.layout(layout -> layout.setWidth(uiWidth));
            }
        }
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (resourceProvider.tickResourceProvider()) {
            reloadResourceContainer();
        }
    }

    protected TreeBuilder.Menu getMenu() {
        var menu = TreeBuilder.Menu.start();
        menu.branch("ldlib.gui.editor.group.size", m -> {
            m.leaf(this.uiWidth == 15 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.small", () -> setUiWidth(15));
            m.leaf(this.uiWidth == 30 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.medium", () -> setUiWidth(30));
            m.leaf(this.uiWidth == 50 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.large", () -> setUiWidth(50));
            m.leaf(this.uiWidth == 100 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.extra_large", () -> setUiWidth(100));
        });
        if (resource.getResourceLocation() != null) {
            menu.leaf(Icons.FOLDER, "ldlib.gui.tips.open_folder", () -> Util.getPlatform().openFile(resource.getResourceLocation()));
        }
        menu.crossLine();
        if (selected != null && canEdit.test(selected)) {
            menu.leaf(Icons.EDIT_FILE, "ldlib.gui.editor.menu.edit", () -> editResource(selected));
        }
        if (selected != null && canRename.test(selected)) {
            menu.leaf("ldlib.gui.editor.menu.rename", () -> renameResource(selected));
        }
        menu.crossLine();
        if (selected != null && canCopy.test(selected) && resource.getResourceLocation() != null) {
            menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> copyResource(selected));
        }
        if (addDefault != null && resource.getResourceLocation() != null) {
            menu.leaf(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", this::addNewResource);
        }
        if (selected != null && canRemove.test(selected)) {
            menu.leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", () -> removeResource(selected));
        }
        if (onMenu != null) {
            onMenu.accept(this, menu);
        }
        return menu;
    }

    protected void addNewResource() {
        if (addDefault != null && resource.getResourceLocation() != null) {
            var newResource = addDefault.get();
            if (newResource != null) {
                addNewResource(newResource);
            }
        }
    }

    public void addNewResource(T value) {
        if (value == null || resource.getResourceLocation() == null) return;
        var key = IResourcePath.file(new File(resource.getResourceLocation(), "new resource" + resource.getFileResourceSuffix()));
        var count = 1;
        while (resource.hasResource(key)) {
            key = IResourcePath.file(new File(resource.getResourceLocation(), "new resource" + count + resource.getFileResourceSuffix()));
            count++;
        }
        resource.addResource(key, value);
        var ui = createResourceUI(key);
        resourceUIs.put(key, ui);
        scrollerView.addScrollViewChild(ui);
        selectResource(key);
    }

    public void copyResource(IResourcePath key) {
        if (key != null && canCopy.test(key) && resource.getResourceLocation() != null) {
            var value = resource.getResource(key);
            if (value != null) {
                var tag = resource.serialize(value, Platform.getFrozenRegistry());
                if (tag != null) {
                    var copied = resource.deserialize(tag, Platform.getFrozenRegistry());
                    if (copied != null) {
                        var count = 1;
                        if (key.isBuiltinResource()) {
                            key = IResourcePath.file(new File(resource.getResourceLocation(), key.getResourceName() + resource.getFileResourceSuffix()));
                        }
                        var newKey = key.resolve("copy");
                        while(resource.hasResource(newKey)) {
                            newKey = key.resolve("copy" + count);
                            count++;
                        }
                        resource.addResource(newKey, copied);
                        var ui = createResourceUI(newKey);
                        resourceUIs.put(key, ui);
                        scrollerView.addScrollViewChild(ui);
                    }
                }
            }
        }
    }

    public void removeResource(IResourcePath key) {
        if (key != null && canRemove.test(key)) {
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

    public void editResource(IResourcePath key) {
        if (key != null && canEdit.test(key)) {
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
                    var newPath = key.rename(newName);
                    if (newPath.equals(key)) {
                        // if the name is the same, just update the label
                        label.setText(nameSupplier.apply(key));
                        label.removeChild(textField);
                        return;
                    }
                    // find a unique name
                    var count = 0;
                    while (resource.hasResource(newPath)) {
                        count++;
                        newPath = key.rename(newName + " (" + count + ")");
                    }
                    resource.addResource(newPath, resource.getResource(key));
                    resource.removeResource(key);
                    removeResource(key);
                    var newUI = createResourceUI(newPath);
                    resourceUIs.put(newPath, newUI);
                    scrollerView.addScrollViewChild(newUI);
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
