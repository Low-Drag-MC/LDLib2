package com.lowdragmc.lowdraglib2.editor.ui.resource;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import org.jetbrains.annotations.Nullable;
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
    protected UIElementProvider<IResourcePath> uiSupplier = path -> new UIElement().layout(layout -> {
        layout.widthPercent(100);
        layout.heightPercent(100);
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
    protected Function<IResourcePath, ?> onDragProvider;
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
    @Setter
    @Nullable
    protected Consumer<T> onResourceSelect = null;
    // runtime
    @Getter
    protected HashSet<IResourcePath> dirtyResources = new HashSet<>();
    @Getter @Nullable
    protected IResourcePath selected = null;
    @Getter @Setter
    protected Editor editor;
    @Nullable
    protected IResourcePath lastClickPath;

    public ResourceProviderContainer(IResourceProvider<T> resourceProvider) {
        getLayout().widthPercent(100);
        getLayout().flex(1);
        this.resourceProvider = resourceProvider;
        this.nameSupplier = resourceProvider::getResourceName;
        this.canRemove = resourceProvider::canRemove;
        this.canRename = resourceProvider::canRename;
        this.canEdit = resourceProvider::canEdit;
        this.canCopy = resourceProvider::canCopy;
        this.supportAdd = resourceProvider::supportAdd;
        this.onDragProvider = resourceProvider::getResource;

        this.scrollerView.scrollerStyle(style -> {
            style.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.ALWAYS);
        }).layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        this.scrollerView.viewContainer.layout(layout -> {
           layout.flexDirection(FlexDirection.ROW);
           layout.wrap(FlexWrap.WRAP);
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
            if (resourceProvider.getResourceInstance().getDisplayMode() == Resource.DisplayMode.LIST) {
                layout.widthPercent(100);
                layout.flexDirection(FlexDirection.ROW);
                layout.marginVertical(1);
            } else {
                layout.width(resourceProvider.getResourceInstance().getUiWidth());
                layout.flexDirection(FlexDirection.COLUMN);
                layout.marginAll(3);
            }
            layout.gapAll(2);
        }).addChildren(new UIElement().layout(layout -> {
                    layout.width(resourceProvider.getResourceInstance().getUiWidth());
                    layout.height(resourceProvider.getResourceInstance().getUiWidth());
                    layout.alignItems(AlignItems.CENTER);
                    layout.justifyContent(AlignContent.CENTER);
        }).addChild(uiSupplier.apply(key)), new Label().textStyle(style -> {
            style.font(LDLibFonts.JETBRAINS_MONO_BOLD);
            if (resourceProvider.getResourceInstance().getDisplayMode() == Resource.DisplayMode.LIST) {
                style.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL);
                style.fontSize(9);
            } else {
                style.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL);
                style.fontSize(5);
            }
        }).setText(nameSupplier.apply(key)).setOverflowVisible(false).layout(layout -> {
            if (resourceProvider.getResourceInstance().getDisplayMode() == Resource.DisplayMode.LIST) {
                layout.flex(1);
                layout.heightPercent(100);
                layout.justifyContent(AlignContent.CENTER);
            } else {
                layout.height(14);
            }
        }))
                .addEventListener(UIEvents.MOUSE_DOWN, e -> selectResource(key))
                .addEventListener(UIEvents.DOUBLE_CLICK, e-> editResource(key))
                .addEventListener(UIEvents.MOUSE_DOWN, e -> {
                    if (e.button == 0) {
                        lastClickPath = key;
                    }
                }).addEventListener(UIEvents.MOUSE_UP, e -> {
                    lastClickPath = null; // Reset click time
                }).addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                    if (lastClickPath == key && isMouseDown(0)) {
                        e.currentElement.startDrag(onDragProvider.apply(key), new TextTexture(nameSupplier.apply(key)));
                    }
                    lastClickPath = null;
                }, true);
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
        var res = resourceProvider.getResource(resourcePath);
        if (onResourceSelect != null && res != null) {
            onResourceSelect.accept(res);
        }
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
        if (resourceProvider.getResourceInstance().getUiWidth() != uiWidth && uiWidth > 0) {
            resourceProvider.getResourceInstance().setUiWidth(uiWidth);
            reloadResourceContainer();
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
        if (resourceProvider.checkAndUpdateResourceProvider()) {
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

    public void setDisplayMode(Resource.DisplayMode mode) {
        if (resourceProvider.getResourceInstance().getDisplayMode() != mode) {
            resourceProvider.getResourceInstance().setDisplayMode(mode);
            reloadResourceContainer();
        }
    }

    protected TreeBuilder.Menu getMenu() {
        var menu = TreeBuilder.Menu.start();
        var isList = resourceProvider.getResourceInstance().getDisplayMode() == Resource.DisplayMode.LIST;
        var uiWidth = resourceProvider.getResourceInstance().getUiWidth();
        menu.leaf(isList ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.list", () -> setDisplayMode(isList ?
                Resource.DisplayMode.GRID : Resource.DisplayMode.LIST));
        menu.branch("ldlib.gui.editor.group.size", m -> {
            m.leaf(uiWidth == 15 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.small", () -> setUiWidth(15));
            m.leaf(uiWidth == 30 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.medium", () -> setUiWidth(30));
            m.leaf(uiWidth == 50 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.large", () -> setUiWidth(50));
            m.leaf(uiWidth == 100 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.extra_large", () -> setUiWidth(100));
        });
        menu.crossLine();
        if (selected != null) {
            menu.leaf("ldlib.gui.editor.menu.copy_path", () ->
                    ClipboardManager.INSTANCE.copyDirect(selected.getPathWithType())
            );
        }
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
        var key = resourceProvider.createSubPath("new_res");
        var count = 1;
        while (resourceProvider.hasResource(key)) {
            key = resourceProvider.createSubPath("new_res_" + count);
            count++;
        }
        IResourcePath finalKey = key;
        editor.historyView.pushHistory(Component.translatable("editor.new_resource"), EditAction.of(
                () -> addResourceInternal(value, finalKey), () -> removeResourceInternal(finalKey)));
    }

    public void copyResource(IResourcePath key) {
        if (key != null && canCopy.test(key)) {
            var value = resourceProvider.getResource(key);
            if (value != null) {
                var tag = resourceProvider.getResourceInstance().resource.serializeResource(value, Platform.getFrozenRegistry());
                if (tag != null) {
                    var copied = resourceProvider.getResourceInstance().resource.deserializeResource(tag, Platform.getFrozenRegistry());
                    if (copied != null) {
                        var count = 1;
                        var newKey = resourceProvider.createSubPath(resourceProvider.getResourceName(key) + "_copy");
                        while(resourceProvider.hasResource(newKey)) {
                            newKey = resourceProvider.createSubPath(resourceProvider.getResourceName(key) + "_copy_" + count);
                            count++;
                        }
                        IResourcePath finalNewKey = newKey;
                        editor.historyView.pushHistory(Component.translatable("editor.copy_resource"), EditAction.of(
                                () -> addResourceInternal(copied, finalNewKey),
                                () -> removeResourceInternal(finalNewKey)));

                    }
                }
            }
        }
    }

    public void removeResource(IResourcePath key, boolean confirm) {
        if (key != null && canRemove.test(key)) {
            var value = resourceProvider.getResource(key);
            if (value == null) return;
            if (confirm) {
                Dialog.showCheckBox("ldlib.gui.editor.menu.remove", "editor.remove.confirm", result -> {
                    if (result) {
                        editor.historyView.pushHistory(Component.translatable("editor.remove_resource"), EditAction.of(
                                () -> removeResourceInternal(key),
                                () -> addResourceInternal(value, key)));
                    }
                }).show(editor);
            } else {
                editor.historyView.pushHistory(Component.translatable("editor.remove_resource"), EditAction.of(
                        () -> removeResourceInternal(key),
                        () -> addResourceInternal(value, key)));
            }
        }
    }

    private void addResourceInternal(T value, IResourcePath finalKey) {
        resourceProvider.addResource(finalKey, value);
        appendResourceUI(finalKey);
        selectResource(finalKey);
    }

    private void removeResourceInternal(IResourcePath key) {
        resourceProvider.removeResource(key);
        var ui = resourceUIs.remove(key);
        if (ui != null) {
            scrollerView.removeScrollViewChild(ui);
        }
        if (selected == key) {
            selected = null;
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
                var textField = new TextField().setText(nameSupplier.apply(key)).setCharValidator(ResourceLocation::isAllowedInResourceLocation);
                textField.addEventListener(UIEvents.BLUR, e -> {
                    var newName = textField.getText().trim();
                    var newPath = resourceProvider.createSubPath(newName);
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
                        newPath = resourceProvider.createSubPath(newName + " (" + count + ")");
                    }
                    onRename(key, newPath);
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

    protected void onRename(IResourcePath oldPath, IResourcePath newPath) {
        editor.historyView.pushHistory(Component.translatable("editor.rename_resource"), EditAction.of(() -> {
            resourceProvider.addResource(newPath, resourceProvider.getResource(oldPath));
            resourceProvider.removeResource(oldPath);
            removeResource(oldPath, false);
            appendResourceUI(newPath);
            selectResource(newPath);
        }, () -> {
            resourceProvider.addResource(oldPath, resourceProvider.getResource(newPath));
            resourceProvider.removeResource(newPath);
            removeResource(newPath, false);
            appendResourceUI(oldPath);
            selectResource(oldPath);
        }));
    }

}
