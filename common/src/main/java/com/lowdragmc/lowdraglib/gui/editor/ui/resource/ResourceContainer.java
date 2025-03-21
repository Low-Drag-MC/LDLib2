package com.lowdragmc.lowdraglib.gui.editor.ui.resource;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.datafixers.util.Either;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.Util;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.function.TriFunction;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.*;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote ResourceContainer
 */
@Accessors(chain = true)
public class ResourceContainer<T, C extends Widget> extends WidgetGroup {
    @Getter
    protected final ResourcePanel panel;
    @Getter
    protected final Resource<T> resource;
    @Getter
    protected final Map<Either<String, File>, C> widgets;
    protected DraggableScrollableWidgetGroup container;
    @Setter
    @Getter
    protected Function<Either<String, File>, C> widgetSupplier;
    @Setter
    protected Function<String, T> onAdd;
    @Setter
    protected Predicate<Either<String, File>> canRemove;
    @Setter
    protected Consumer<Either<String, File>> onRemove;
    @Setter
    protected Predicate<Either<String, File>> canGlobalChange;
    @Setter
    protected Consumer<Either<String, File>> onGlobalChange;
    @Setter
    protected Predicate<Either<String, File>> canEdit;
    @Setter
    protected Consumer<Either<String, File>> onEdit;
    @Setter
    protected BiConsumer<Either<String, File>, TreeBuilder.Menu> onMenu;
    protected Function<Either<String, File>, Object> draggingMapping;
    protected TriFunction<Either<String, File>, Object, Position, IGuiTexture> draggingRenderer;
    @Setter
    protected Supplier<String> nameSupplier;
    @Setter
    protected Predicate<String> renamePredicate;
    // runtime
    @Getter
    @Nullable
    protected Either<String, File> selected;
    private boolean firstClick;
    private Either<String, File> firstClickName;
    private long firstClickTime;

    public ResourceContainer(Resource<T> resource, ResourcePanel panel) {
        super(3, 0, panel.getSize().width - 6, panel.getSize().height - 14);
        setClientSideWidget();
        this.widgets = new HashMap<>();
        this.panel = panel;
        this.resource = resource;
    }

    public <D> ResourceContainer<T, C> setDragging(Function<Either<String, File>, D> draggingMapping, Function<D, IGuiTexture> draggingRenderer) {
        this.draggingMapping = draggingMapping::apply;
        this.draggingRenderer = (k, o, p) -> draggingRenderer.apply((D) o);
        return this;
    }

    public <D> ResourceContainer<T, C> setDragging(Function<Either<String, File>, D> draggingMapping, TriFunction<Either<String, File>, D, Position, IGuiTexture> draggingRenderer) {
        this.draggingMapping = draggingMapping::apply;
        this.draggingRenderer = (k, o, p) -> draggingRenderer.apply(k, (D) o, p);
        return this;
    }

    @Override
    public void initWidget() {
        Size size = getSize();
        container = new DraggableScrollableWidgetGroup(1, 2, size.width - 2, size.height - 2);
        container.setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2));
        addWidget(container);
        reBuild();
        super.initWidget();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (gui.getTickCount() % 20 == 0) {
            if (selected != null && selected.right().isPresent()) {
                // check if the file should be updated
                var selectedResource = resource.getResource(selected);
                var resTag = resource.serialize(selectedResource);
                Tag fileTag = EndTag.INSTANCE;
                try {
                    var fileData = NbtIo.read(selected.right().get());
                    if (fileData != null && fileData.getString("type").equals(resource.name()) && fileData.contains("data")) {
                        fileTag = fileData.get("data");
                    }
                } catch (IOException ignored) {}
                if (!fileTag.equals(resTag)) {
                    resource.addStaticResource(selected.right().get(), selectedResource);
                }
            }
            if (resource.loadAndUpdateStaticResource()) {
                reBuild();
            }
        }
    }

    public void reBuild() {
        selected = null;
        container.clearAllWidgets();
        int width = getSize().getWidth();
        int x = 1;
        int y = 3;
        for (var entry : resource.allResources().toList()) {
            var widget = widgetSupplier.apply(entry.getKey());
            var key = entry.getKey();
            widgets.put(key, widget);
            Size size = widget.getSize();
            SelectableWidgetGroup selectableWidgetGroup = new SelectableWidgetGroup(0, 0, size.width, size.height + 14);
            selectableWidgetGroup.setDraggingProvider(draggingMapping == null ? entry::getValue : () -> draggingMapping.apply(key), (c, p) -> draggingRenderer == null ? new TextTexture(resource.getResourceName(key)) : draggingRenderer.apply(key, c, p));
            selectableWidgetGroup.addWidget(widget);
            if ((resource.supportStaticResource() && (canGlobalChange == null || canGlobalChange.test(key)))) {
                selectableWidgetGroup.addWidget(new ImageWidget(1, 1, 10, 10,
                        (IGuiTexture) key.map(l -> Icons.LOCAL, r -> Icons.GLOBAL.copy().setDynamicColor(ColorPattern::generateRainbowColor)))
                        .setHoverTooltips(key.left().isPresent() ? "ldlib.gui.editor.menu.resource.builtin" : "ldlib.gui.editor.menu.resource.static"));
            }
            selectableWidgetGroup.addWidget(new ImageWidget(0, size.height + 3, size.width, 10, new TextTexture(resource.getResourceName(key)).setWidth(size.width).setType(TextTexture.TextType.ROLL)));
            selectableWidgetGroup.setOnSelected(s -> selected = key);
            selectableWidgetGroup.setOnUnSelected(s -> selected = null);
            selectableWidgetGroup.setSelectedTexture(ColorPattern.T_GRAY.rectTexture());
            size = selectableWidgetGroup.getSize();

            if (size.width >= width - 5) {
                selectableWidgetGroup.setSelfPosition(new Position(0, y));
                y += size.height + 3;
            } else if (size.width < width - 5 - x) {
                selectableWidgetGroup.setSelfPosition(new Position(x, y));
                x += size.width + 3;
            } else {
                y += size.height + 3;
                x = 1;
                selectableWidgetGroup.setSelfPosition(new Position(x, y));
                x += size.width + 3;
            }
            container.addWidget(selectableWidgetGroup);
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var result = super.mouseClicked(mouseX, mouseY, button);
        if (button == 1 && isMouseOverElement(mouseX, mouseY)) {
            panel.getEditor().openMenu(mouseX, mouseY, getMenu());
            return true;
        } else if (button == 0 && isMouseOverElement(mouseX, mouseY) && selected != null && onEdit != null && (canEdit == null || canEdit.test(selected))) {
            if (firstClick && firstClickName.equals(selected) && gui.getTickCount() - firstClickTime < 10) {
                editResource();
                firstClick = false;
                return true;
            }
            firstClick = true;
            firstClickName = selected;
            firstClickTime = gui.getTickCount();
        }
        return result;
    }

    protected TreeBuilder.Menu getMenu() {
        var menu = TreeBuilder.Menu.start();
        if (onEdit != null && (canEdit != null && canEdit.test(selected))) {
            menu.leaf(Icons.EDIT_FILE, "ldlib.gui.editor.menu.edit", this::editResource);
        }
        menu.leaf("ldlib.gui.editor.menu.rename", this::renameResource);
        menu.crossLine();
        if (resource.supportStaticResource()) {
            menu.leaf(Icons.FOLDER, "ldlib.gui.editor.menu.static_resource.folder", () -> Util.getPlatform().openFile(resource.getStaticLocation()));
            if (selected != null && (canGlobalChange == null || canGlobalChange.test(selected))) {
                if (selected.left().isPresent()) {
                    menu.leaf(Icons.GLOBAL, "ldlib.gui.editor.menu.resource.builtin_to_static", () -> {
                        var preName = selected;
                        var name = resource.getResourceName(selected);
                        var value = resource.getResource(selected);
                        if (value != null) {
                            resource.removeResource(selected);
                            resource.addStaticResource(resource.getStaticResourceFile(name), value);
                            reBuild();
                        }
                        if (onGlobalChange != null) {
                            onGlobalChange.accept(preName);
                        }
                    });
                } else {
                    menu.leaf(Icons.LOCAL, "ldlib.gui.editor.menu.resource.static_to_builtin", () -> {
                        var preName = selected;
                        var name = resource.getResourceName(selected);
                        var value = resource.getResource(selected);
                        if (value != null) {
                            resource.removeResource(selected);
                            resource.addBuiltinResource(name, value);
                            reBuild();
                        }
                        if (onGlobalChange != null) {
                            onGlobalChange.accept(preName);
                        }
                    });
                }
            }
            menu.crossLine();
        }
        menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", this::copy);
        menu.leaf(Icons.PASTE, "ldlib.gui.editor.menu.paste", this::paste);
        if (onAdd != null) {
            menu.leaf(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", this::addNewResource);
        }
        menu.leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", this::removeSelectedResource);
        if (onMenu != null) {
            onMenu.accept(selected, menu);
        }
        return menu;
    }

    protected void paste() {
        panel.getEditor().ifCopiedPresent(resource.name(), c -> {
            var value = getResource().deserialize((Tag) c);
            resource.addBuiltinResource(genNewFileName(), value);
            reBuild();
        });
    }

    protected void copy() {
        if (selected != null) {
            panel.getEditor().setCopy(resource.name(), resource.serialize(resource.getResource(selected)));
        }
    }

    protected void renameResource() {
        if (selected != null) {
            DialogWidget.showStringEditorDialog(Editor.INSTANCE, LocalizationUtils.format("ldlib.gui.editor.tips.rename") + " " + LocalizationUtils.format(resource.name()),
                    resource.getResourceName(selected), s -> {
                        if (!selected.map(l -> resource.hasBuiltinResource(s), r -> resource.hasStaticResource(resource.getStaticResourceFile(s)))) {
                            return false;
                        }
                        if (renamePredicate != null) {
                            return renamePredicate.test(s);
                        }
                        return true;
                    }, s -> {
                        if (s == null) return;
                        var stored = resource.removeResource(selected);
                        if (stored != null) {
                            var name = selected.mapBoth(l -> s, r -> resource.getStaticResourceFile(s));
                            resource.addResource(name, stored);
                        }
                        reBuild();
                    });
        }
    }

    protected void editResource() {
        if (onEdit != null && selected != null && (canEdit == null || canEdit.test(selected))) {
            onEdit.accept(selected);
        }
    }

    protected String genNewFileName() {
        String randomName = "new ";
        if (nameSupplier != null) {
            randomName = nameSupplier.get();
        } else {
            int i = 0;
            while (resource.hasBuiltinResource(randomName + i)) {
                i++;
            }
            randomName += i;
        }
        return randomName;
    }

    protected void addNewResource() {
        if (onAdd != null) {
            String randomName = genNewFileName();
            resource.addBuiltinResource(randomName, onAdd.apply(randomName));
            reBuild();
        }
    }

    protected void removeSelectedResource() {
        if (selected == null) return;
        if (canRemove == null || canRemove.test(selected)) {
            if (onRemove != null) {
                onRemove.accept(selected);
            }
            resource.removeResource(selected);
            reBuild();
        }
    }

}
