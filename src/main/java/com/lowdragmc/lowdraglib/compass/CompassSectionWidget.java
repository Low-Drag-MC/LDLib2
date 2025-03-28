package com.lowdragmc.lowdraglib.compass;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.shader.Shaders;
import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.math.Position;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.Pair;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2022/8/26
 * @implNote BookTabGroup
 */
public class CompassSectionWidget extends WidgetGroup {

    protected enum Mode {
        CURSOR(Icons.CURSOR),
        MOVE(Icons.MOVE),
        LINK(Icons.LINK);
        final IGuiTexture icon;

        Mode(IGuiTexture icon) {
            this.icon = icon;
        }
    }

    protected final CompassView compassView;
    protected final CompassSection section;
    protected float xOffset, yOffset;
    protected float scale = 1;
    protected double lastMouseX, lastMouseY;
    protected boolean isDragging = false;
    protected boolean editMode = false;
    protected WidgetGroup editModeWidget;
    protected Mode mode = Mode.CURSOR;
    protected int gridWidth = 50;
    protected boolean magnetic = true;
    @Nullable
    protected CompassNode selectedNode = null;
    protected Position originPosition = Position.ORIGIN;
    protected Pair<CompassNode, CompassNode> selectedLink = null;
    protected Set<CompassNode> removedNodes = new HashSet<>();

    public CompassSectionWidget(CompassView compassView, CompassSection section) {
        super(0, 0, compassView.getSize().width - CompassView.LIST_WIDTH, compassView.getSize().height);
        this.compassView = compassView;
        this.section = section;
        this.resetFitScale();
        // Reset View
        addWidget(new ButtonWidget(10, 10, 20, 20,
                new GuiTextureGroup(ColorPattern.T_GRAY.rectTexture(), Icons.ROTATION),
                cd -> resetFitScale()).setHoverTooltips(Component.translatable("ldlib.gui.compass.reset_view")));

        // Edit Mode
        if (CompassManager.INSTANCE.devMode) {
            addWidget(new SwitchWidget(40, 10, 20, 20, (cd, isPressed) -> setEditMode(isPressed))
                    .setSupplier(() -> editMode)
                    .setTexture(new GuiTextureGroup(ColorPattern.T_GRAY.rectTexture(), Icons.EDIT_OFF),
                            new GuiTextureGroup(ColorPattern.T_CYAN.rectTexture(), Icons.EDIT_ON))
                    .setHoverTooltips(Component.translatable("ldlib.gui.compass.edit_mode")));
        }

        addWidget(editModeWidget = new WidgetGroup(0, 0, getSize().width, getSize().height));
        editModeWidget.setVisible(false);
        editModeWidget.setActive(false);

        editModeWidget.addWidget(new ButtonWidget(40, 30, 20, 20,
                new GuiTextureGroup(ColorPattern.T_RED.rectTexture(), Icons.SAVE), cd -> saveSection())
                .setHoverTooltips(Component.translatable("ldlib.gui.editor.menu.save")));

        TextBoxWidget textBox;;
        editModeWidget.addWidget(textBox = new TextBoxWidget(getSize().width - 200, 10, 200,
                List.of("ldlib.gui.compass.mode.%s.tooltip".formatted(mode.name().toLowerCase())))
                .setFontColor(-1)
                .setShadow(true));

        for (Mode mode : Mode.values()) {
            editModeWidget.addWidget(new ImageWidget(62 + 16 * mode.ordinal(), 32, 16, 16, () -> this.mode == mode ? ResourceBorderTexture.SELECTED.copy().setColor(ColorPattern.RED.color) : IGuiTexture.EMPTY));
            editModeWidget.addWidget(new ButtonWidget(62 + 16 * mode.ordinal(), 32, 16, 16, mode.icon, cd -> {
                switchMode(mode);
                textBox.setContent(List.of("ldlib.gui.compass.mode.%s.tooltip".formatted(mode.name().toLowerCase())));
            })
                    .setHoverTexture(new GuiTextureGroup(ResourceBorderTexture.SELECTED.copy().setColor(ColorPattern.T_RED.color), mode.icon))
                    .setHoverTooltips(Component.translatable("ldlib.gui.compass.mode." + mode.name().toLowerCase())));
        }

    }

    private void saveSection() {
        var path = new File(LDLib.getLDLibDir(), "assets/%s/compass/nodes".formatted(section.getSectionName().getNamespace()));
        if (!path.isDirectory()) {
            if (path.mkdirs()) {
                LDLib.LOGGER.info("Created directory %s".formatted(path));
            } else {
                LDLib.LOGGER.error("Failed to create directory %s".formatted(path));
                return;
            }
        }

        for (CompassNode removedNode : removedNodes) {
            var file = new File(path, removedNode.getNodeName().getPath() + ".json");
            if (file.exists()) {
                if (!file.delete()) {
                    LDLib.LOGGER.error("Failed to delete file %s".formatted(file));
                }
            }
        }
        removedNodes.clear();

        for (CompassNode node : section.nodes.values()) {
            var file = new File(path, node.getNodeName().getPath() + ".json");
            FileUtility.saveJson(file, node.updateJson());
        }

        DialogWidget.showCheckBox(compassView, "ldlib.gui.compass.save_success", "ldlib.gui.compass.save_success.desc", shouldOpen -> {
            if (shouldOpen) {
                Util.getPlatform().openFile(path);
            }
        });

        CompassManager.INSTANCE.onResourceManagerReload(Minecraft.getInstance().getResourceManager());
    }

    protected void setEditMode(boolean editMode) {
        this.editMode = editMode;
        this.removedNodes.clear();
        editModeWidget.setVisible(editMode);
        editModeWidget.setActive(editMode);
    }

    protected void switchMode(Mode mode) {
        this.mode = mode;
        this.selectedNode = null;
        this.selectedLink = null;
    }

    protected void resetFitScale() {
        int minX, minY, maxX, maxY;
        if (this.section.nodes.isEmpty()) {
            this.xOffset = 0;
            this.yOffset = 0;
            this.scale = 1;
            return;
        }
        minX = minY = Integer.MAX_VALUE;
        maxX = maxY = Integer.MIN_VALUE;
        for (CompassNode node : this.section.nodes.values()) {
            Position position = node.getPosition();
            minX = Math.min(minX, position.x - node.size);
            minY = Math.min(minY, position.y - node.size);
            maxX = Math.max(maxX, position.x + node.size);
            maxY = Math.max(maxY, position.y + node.size);
        }
        this.xOffset = minX;
        this.yOffset = minY;
        var scaleWidth = (float) getSize().width / (maxX - minX);
        var scaleHeight = (float) getSize().height / (maxY - minY);
        this.scale = Math.min(scaleWidth, scaleHeight);
        if (scale < 0.5f) {
            this.scale = 0.5f;
        }
        this.xOffset -= (getSize().width / scale - (maxX - minX)) / 2;
        this.yOffset -= (getSize().height / scale - (maxY - minY)) / 2;
    }

    @Nullable
    protected TreeBuilder.Menu createMenu(double mouseX, double mouseY) {
        var menu = TreeBuilder.Menu.start()
                .branch(Icons.GRID, "ldlib.gui.compass.grid_size", m -> m
                        .leaf(gridWidth == 0 ? Icons.CHECK : IGuiTexture.EMPTY, "close", () -> gridWidth = 0)
                        .leaf(gridWidth == 10 ? Icons.CHECK : IGuiTexture.EMPTY, "10×10", () -> gridWidth = 10)
                        .leaf(gridWidth == 25 ? Icons.CHECK : IGuiTexture.EMPTY, "25×25", () -> gridWidth = 25)
                        .leaf(gridWidth == 50 ? Icons.CHECK : IGuiTexture.EMPTY, "50×50", () -> gridWidth = 50)
                        .leaf(gridWidth == 100 ? Icons.CHECK : IGuiTexture.EMPTY, "100×100", () -> gridWidth = 100)
                        .leaf(gridWidth == 150 ? Icons.CHECK : IGuiTexture.EMPTY, "150×150", () -> gridWidth = 200));
        return switch (mode) {
            case CURSOR -> {
                menu.crossLine().leaf(Icons.ADD, "ldlib.gui.compass.add_node", () -> {
                    var newId = 0;
                    while (section.nodes.containsKey(ResourceLocation.parse("%s:%s/new_node_%d".formatted(section.sectionName.getNamespace(), section.sectionName.getPath(), newId)))) {
                        newId++;
                    }
                    var id = ResourceLocation.parse("%s:%s/new_node_%d".formatted(section.sectionName.getNamespace(), section.sectionName.getPath(), newId));
                    DialogWidget.showStringEditorDialog(compassView, "ldlib.gui.editor.tips.add_node", id.toString(),
                            s -> LDLib.isValidResourceLocation(s) && !section.nodes.containsKey(ResourceLocation.parse(s)), s -> {
                                if (s == null || !LDLib.isValidResourceLocation(s) || section.nodes.containsKey(ResourceLocation.parse(s))) return;
                                int newMouseX = (int) ((mouseX - this.getPosition().x) / scale + xOffset);
                                int newMouseY = (int) ((mouseY - this.getPosition().y) / scale + yOffset);
                                var config = LDLib.GSON.fromJson("""
                                        {
                                          "section": "%s",
                                          "button_texture": {
                                            "type": "item",
                                            "res": "minecraft:tnt"
                                          },
                                          "position": [
                                            %d,
                                            %d
                                          ],
                                          "page": "%s",
                                          "items": [
                                          ]
                                        }
                                        """.formatted(section.sectionName.toString(), newMouseX, newMouseY, s), JsonObject.class);
                                var node = new CompassNode(ResourceLocation.parse(s), config);
                                node.setSection(section);
                                section.nodes.put(node.getNodeName(), node);
                            });
                });
                if (selectedNode == null) yield menu;
                menu.leaf(Icons.REMOVE, "ldlib.gui.compass.remove_node", () -> {
                    if (selectedNode != null) {
                        section.nodes.remove(selectedNode.getNodeName());
                        for (CompassNode node : section.nodes.values()) {
                            node.preNodes.remove(selectedNode);
                            node.childNodes.remove(selectedNode);
                        }
                        removedNodes.add(selectedNode);
                        selectedNode = null;
                    }
                }).leaf("ldlib.gui.editor.menu.rename", () -> {
                    if (selectedNode != null) {
                        DialogWidget.showStringEditorDialog(compassView, "ldlib.gui.editor.tips.rename", selectedNode.getNodeName().toString(),
                                ResourceLocation::isValidPath, s -> {
                                    if (s == null || !LDLib.isValidResourceLocation(s)) return;
                                    selectedNode.setNodeName(ResourceLocation.parse(s));
                                });
                    }
                }).branch("ldlib.gui.editor.group.size", m -> m
                        .leaf(selectedNode.size == 20 ? Icons.CHECK : IGuiTexture.EMPTY, "20", () -> selectedNode.size = 20)
                        .leaf(selectedNode.size == 24 ? Icons.CHECK : IGuiTexture.EMPTY, "24", () -> selectedNode.size = 24)
                        .leaf(selectedNode.size == 30 ? Icons.CHECK : IGuiTexture.EMPTY, "30", () -> selectedNode.size = 30)
                        .leaf(selectedNode.size == 40 ? Icons.CHECK : IGuiTexture.EMPTY, "40", () -> selectedNode.size = 40)
                        .leaf(selectedNode.size == 60 ? Icons.CHECK : IGuiTexture.EMPTY, "60", () -> selectedNode.size = 60)
                        .leaf(selectedNode.size == 80 ? Icons.CHECK : IGuiTexture.EMPTY, "80", () -> selectedNode.size = 80)
                        .leaf(selectedNode.size == 100 ? Icons.CHECK : IGuiTexture.EMPTY, "100", () -> selectedNode.size = 100)
                        .leaf(selectedNode.size == 150 ? Icons.CHECK : IGuiTexture.EMPTY, "150", () -> selectedNode.size = 150)
                ).branch("ldlib.gui.compass.attach_items", m -> {
                    m.branch(Icons.ADD, "ldlib.gui.editor.tips.add_item", m2 -> m2
                            .leaf("ldlib.gui.compass.attach_items.item", () -> DialogWidget.showItemSelector(compassView, "ldlib.gui.editor.tips.add_item", ItemStack.EMPTY, item -> {
                                if (item != null && item != Items.AIR) {
                                    var items = GsonHelper.getAsJsonArray(selectedNode.config, "items", new JsonArray());
                                    items.add(BuiltInRegistries.ITEM.getKey(item).toString());
                                    selectedNode.config.add("items", items);
                                }
                            }))
                            .leaf("ldlib.gui.compass.attach_items.tag", () -> DialogWidget.showStringEditorDialog(compassView, "ldlib.gui.compass.attach_items.tag", "minecraft:planks", ResourceLocation::isValidPath, s -> {
                                if (s == null || !LDLib.isValidResourceLocation(s)) return;
                                var items = GsonHelper.getAsJsonArray(selectedNode.config, "items", new JsonArray());
                                items.add("#"+s);
                                selectedNode.config.add("items", items);
                            })));
                    var items = GsonHelper.getAsJsonArray(selectedNode.config, "items", new JsonArray());
                    if (!items.isEmpty()) {
                        m.crossLine();
                        for (var element : items) {
                            var data = element.getAsString();
                            if (LDLib.isValidResourceLocation(data)) {
                                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data));
                                if (item != Items.AIR) {
                                    m.leaf(new ItemStackTexture(item), LocalizationUtils.format("ldlib.gui.editor.tips.remove_item") + ": " + LocalizationUtils.format(item.getDescriptionId()), () -> items.remove(element));
                                }
                            } else if (data.startsWith("#") && LDLib.isValidResourceLocation(data.substring(1))) {
                                var tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(data.substring(1)));
                                var tagCollection = BuiltInRegistries.ITEM.getTag(tag);
                                tagCollection.ifPresent(named -> {
                                    var itemList = new ArrayList<Item>();
                                    named.forEach(holder -> itemList.add(holder.value()));
                                    m.leaf(new ItemStackTexture(itemList.stream().map(ItemStack::new).toArray(ItemStack[]::new)), LocalizationUtils.format("ldlib.gui.editor.tips.remove_tag") + ": " + data, () -> items.remove(element));
                                });
                            }
                        }
                    }
                }).leaf(Icons.EDIT_FILE, "ldlib.gui.editor.menu.edit", () -> {
                    var file = new File(LDLib.getLDLibDir(), "assets/%s/compass/pages/en_us/%s.xml".formatted(selectedNode.getNodeName().getNamespace(), selectedNode.getNodeName().getPath()));
                    if (!file.exists()) {
                        if (!file.getParentFile().isDirectory()) {
                            file.getParentFile().mkdirs();
                        }
                        var pageLocation = selectedNode.getPage();
                        var resourceManager = Minecraft.getInstance().getResourceManager();
                        var path = "compass/pages/en_us/%s.xml".formatted(pageLocation.getPath());
                        var option = resourceManager.getResource(ResourceLocation.fromNamespaceAndPath(pageLocation.getNamespace(), path));
                        var resource = option.orElseGet(() -> resourceManager.getResource(LDLib.id("compass/pages/en_us/missing.xml")).orElseThrow());
                        String content;
                        try (var inputStream = resource.open()) {
                            content = FileUtility.readInputStream(inputStream);
                        } catch (Exception e) {
                            content = """
                                        <page>
                                            <h1>Page Title</h1>
                                            <text>
                                                Page Content
                                            </text>
                                        </page>
                                        """;
                        }
                        try (var writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                            writer.write(content);
                        } catch (Exception ignored) {
                            LDLib.LOGGER.error("Failed to create file %s".formatted(file));
                            return;
                        }
                    }
                    Util.getPlatform().openFile(file);
                });
                createTextureMenu(menu, "ldlib.gui.compass.set_button_texture",
                        selectedNode.getButtonTexture(),
                        new ItemStackTexture(Items.TNT),
                        texture -> selectedNode.setButtonTexture(texture));
                createTextureMenu(menu, "ldlib.gui.compass.set_background",
                        Optional.ofNullable(selectedNode.getBackground()).orElse(compassView.config.getNodeBackground()),
                        compassView.config.getNodeBackground(),
                        texture -> selectedNode.setBackground(texture == compassView.config.getNodeBackground() ? null : texture));
                createTextureMenu(menu, "ldlib.gui.compass.set_hover_background",
                        Optional.ofNullable(selectedNode.getHoverBackground()).orElse(compassView.config.getNodeHoverBackground()),
                        compassView.config.getNodeHoverBackground(),
                        texture -> selectedNode.setHoverBackground(texture == compassView.config.getNodeHoverBackground() ? null : texture));
                yield menu;
            }
            case MOVE -> menu.crossLine().leaf(magnetic ? Icons.CHECK : IGuiTexture.EMPTY, "ldlib.gui.compass.magnetic", () -> magnetic = !magnetic);
            case LINK -> selectedLink == null ? menu : menu.crossLine().leaf(Icons.REMOVE, "ldlib.gui.editor.menu.remove", () -> {
                if (selectedLink != null) {
                    selectedLink.left().childNodes.remove(selectedLink.right());
                    selectedLink.right().preNodes.remove(selectedLink.left());
                    selectedLink.left().preNodes.remove(selectedLink.right());
                    selectedLink.right().childNodes.remove(selectedLink.left());
                    selectedLink = null;
                }
            });
        };
    }

    protected void createTextureMenu(TreeBuilder.Menu menu, String title, IGuiTexture initial, IGuiTexture defaultTexture, Consumer<IGuiTexture> consumer) {
        menu.branch(initial, title, m -> m
                .leaf(defaultTexture == initial ? defaultTexture : IGuiTexture.EMPTY, "ldlib.gui.compass.default", () -> consumer.accept(defaultTexture))
                .leaf((defaultTexture != initial && initial instanceof ResourceTexture) ? initial : IGuiTexture.EMPTY, "resource_texture", () -> DialogWidget.showStringEditorDialog(compassView, title, "ldlib:textures/gui/icon.png", ResourceLocation::isValidPath, s -> {
                    if (s != null && LDLib.isValidResourceLocation(s)) {
                        consumer.accept(new ResourceTexture(s));
                    }
                }))
                .leaf((defaultTexture != initial && initial instanceof ItemStackTexture) ? initial : IGuiTexture.EMPTY, "item_texture", () -> DialogWidget.showItemSelector(compassView, title, ItemStack.EMPTY, item -> {
                    if (item != null && item != Items.AIR) {
                        consumer.accept(new ItemStackTexture(item));
                    }
                }))
                .leaf((defaultTexture != initial && initial instanceof ShaderTexture) ? initial : IGuiTexture.EMPTY, "shader_texture", () -> DialogWidget.showStringEditorDialog(compassView, title, "ldlib:compass_node", ResourceLocation::isValidPath, s -> {
                    if (s != null && LDLib.isValidResourceLocation(s)) {
                        consumer.accept(ShaderTexture.createShader(ResourceLocation.parse(s)));
                    }
                })));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (isMouseOverElement(mouseX, mouseY)) {
            if (button == 0) {
                int newMouseX = (int) ((mouseX - this.getPosition().x) / scale + xOffset);
                int newMouseY = (int) ((mouseY - this.getPosition().y) / scale + yOffset);
                if (editMode) {
                    for (CompassNode node : this.section.nodes.values()) {
                        if (isNodeOver(node, newMouseX, newMouseY)) {
                            switch (mode) {
                                case CURSOR -> {
                                    selectedNode = node;
                                    continue;
                                }
                                case MOVE -> {
                                    selectedNode = node;
                                    originPosition = node.getPosition();
                                }
                                case LINK -> {
                                    selectedNode = node;
                                }
                            }
                            return true;
                        }
                    }
                    if (mode == Mode.LINK) {
                        for (CompassNode node : this.section.nodes.values()) {
                            for (CompassNode preNode : node.preNodes) {
                                var vec = new Vector2f(node.getPosition().x - preNode.getPosition().x, node.getPosition().y - preNode.getPosition().y);
                                var mid = new Vector2f((node.getPosition().x + preNode.getPosition().x) / 2f, (node.getPosition().y + preNode.getPosition().y) / 2f);
                                var mouseVec = new Vector2f(newMouseX, newMouseY).sub(mid);
                                var project = new Vector2f(0f, 0f);
                                float l = vec.lengthSquared();
                                if (l != 0.0D) {
                                    project.set(vec).mul(mouseVec.dot(vec) / l);
                                }
                                if (project.lengthSquared() < vec.lengthSquared() / 4f) {
                                    var dist = project.sub(mouseVec).length();
                                    if (dist < 10) {
                                        selectedLink = Pair.of(preNode, node);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                isDragging = true;
            } else if (button == 1) {
                if (editMode) {
                    var menu = createMenu(mouseX, mouseY);
                    if (menu != null) {
                        compassView.waitToAdded(new MenuWidget<>((int) mouseX, (int) mouseY, 14, menu.build())
                                .setNodeTexture(MenuWidget.NODE_TEXTURE)
                                .setLeafTexture(MenuWidget.LEAF_TEXTURE)
                                .setNodeHoverTexture(MenuWidget.NODE_HOVER_TEXTURE)
                                .setCrossLinePredicate(TreeBuilder.Menu::isCrossLine)
                                .setKeyIconSupplier(TreeBuilder.Menu::getIcon)
                                .setKeyNameSupplier(TreeBuilder.Menu::getName)
                                .setOnNodeClicked(TreeBuilder.Menu::handle)
                                .setBackground(MenuWidget.BACKGROUND));
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        var lastSelectedNode = selectedNode;
        isDragging = false;
        if (!editMode || mode != Mode.CURSOR) {
            selectedNode = null;
        }
        if (isMouseOverElement(mouseX, mouseY)) {
            int newMouseX = (int) ((mouseX - this.getPosition().x) / scale + xOffset);
            int newMouseY = (int) ((mouseY - this.getPosition().y) / scale + yOffset);
            if (editMode) {
                if (mode == Mode.LINK && lastSelectedNode != null) {
                    for (CompassNode node : this.section.nodes.values()) {
                        if (lastSelectedNode != node && isNodeOver(node, newMouseX, newMouseY)) {
                            node.preNodes.add(lastSelectedNode);
                            lastSelectedNode.childNodes.add(node);
                            return true;
                        }
                    }
                }
            } else {
                for (CompassNode node : this.section.nodes.values()) {
                    if (isNodeOver(node, newMouseX, newMouseY)) {
                        compassView.openNodeContent(node);
                        return true;
                    }
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            xOffset += (float) (lastMouseX - mouseX) / scale;
            yOffset += (float) (lastMouseY - mouseY) / scale;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        if (selectedNode != null && editMode) {
            if (mode == Mode.MOVE) {
                var x = originPosition.x + (int) ((mouseX - lastMouseX) / scale);
                var y = originPosition.y + (int) ((mouseY - lastMouseY) / scale);
                if (magnetic && !isShiftDown()) {
                    x -= x % 10;
                    y -= y % 10;
                }
                selectedNode.setPosition(Position.of(x, y));
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverElement(mouseX, mouseY)) {
            var newScale = (float) Mth.clamp(scale + scrollY * 0.1f, 0.1f, 10f);
            if (newScale != scale) {
                xOffset += (float) (mouseX - this.getPosition().x) / scale - (float) (mouseX - this.getPosition().x) / newScale;
                yOffset += (float) (mouseY - this.getPosition().y) / scale - (float) (mouseY - this.getPosition().y) / newScale;
                scale = newScale;
            }
        }
        return super.mouseWheelMove(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (isMouseOverElement(mouseX, mouseY)) {
            int newMouseX = (int) ((mouseX - this.getPosition().x) / scale + xOffset);
            int newMouseY = (int) ((mouseY - this.getPosition().y) / scale + yOffset);
            for (CompassNode node : this.section.nodes.values()) {
                if (isNodeOver(node, newMouseX, newMouseY)) {
                    gui.getModularUIGui().setHoverTooltip(List.of(node.getChatComponent()), ItemStack.EMPTY, null, null);
                }
            }
        }
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
    }

    protected boolean isNodeOver(CompassNode node, int newMouseX, int newMouseY) {
        return isMouseOver(node.getPosition().x - node.size / 2, node.getPosition().y - node.size / 2, node.size, node.size, newMouseX, newMouseY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.setBackground(section.getBackgroundTexture() == null ? compassView.config.getSectionBackground() : section.getBackgroundTexture());
        this.drawBackgroundTexture(graphics, mouseX, mouseY);
        var pos = getPosition();
        var size = getSize();
        var trans = graphics.pose().last().pose();
        var realPos = trans.transform(new Vector4f(pos.x, pos.y, 0, 1));
        var realPos2 = trans.transform(new Vector4f(pos.x + size.width, pos.y + size.height, 0, 1));
        graphics.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
        graphics.pose().pushPose();
        graphics.pose().translate(this.getPosition().x, this.getPosition().y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().translate(-xOffset, -yOffset, 0);
        int newMouseX = (int) ((mouseX - this.getPosition().x) / scale + xOffset);
        int newMouseY = (int) ((mouseY - this.getPosition().y) / scale + yOffset);
        // draw edit mode grid
        if (editMode && gridWidth > 0) {
            graphics.drawManaged(() -> {
                var w = size.width / scale;
                var h = size.height / scale;
                var sx = (int) (pos.x / scale + xOffset - w - xOffset % gridWidth);
                var sy = (int) (pos.y / scale + yOffset - h - yOffset % gridWidth);
                sx -= (sx % gridWidth);
                sy -= (sy % gridWidth);
                for (int x = sx; x < sx + 3 * w; x += gridWidth) {
                    for (int y = sy; y < sy + 3 * h; y += gridWidth) {
                        DrawerHelper.drawSolidRect(graphics, x, sy, 1, (int) (3 * h), ColorPattern.T_GRAY.color);
                        DrawerHelper.drawSolidRect(graphics, sx, y, (int) (3 * w), 1, ColorPattern.T_GRAY.color);
                    }
                }
            });
        }
        // draw lines
        for (CompassNode node : this.section.nodes.values()) {
            drawChildLines(graphics, node);
        }
        // draw link
        if (editMode && mode == Mode.LINK && selectedNode != null) {
            DrawerHelper.drawLines(graphics, List.of(new Vec2(selectedNode.getPosition().x, selectedNode.getPosition().y), new Vec2(newMouseX, newMouseY)), ColorPattern.T_GREEN.color, ColorPattern.T_GREEN.color, 1f);
        }
        // draw nodes
        for (CompassNode node : this.section.nodes.values()) {
            drawNode(graphics, newMouseX, newMouseY, node);
        }
        graphics.pose().popPose();
        graphics.disableScissor();
        drawWidgetsBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    protected void drawNode(GuiGraphics graphics, int mouseX, int mouseY, CompassNode node) {
        // draw background
        var nodePosition = node.getPosition();
        boolean isHover = isNodeOver(node, mouseX, mouseY) || node == selectedNode;
        var texture = isHover ?
                (node.getHoverBackground() == null ? compassView.config.getNodeHoverBackground() : node.getHoverBackground()) :
                (node.getBackground() == null ? compassView.config.getNodeBackground() : node.getBackground());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
//        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        texture.draw(graphics, mouseX, mouseY, nodePosition.x - node.size / 2f, nodePosition.y - node.size / 2f, node.size, node.size);
        node.getButtonTexture().draw(graphics, mouseX, mouseY, nodePosition.x - node.size * 8f / 24, nodePosition.y - node.size * 8f / 24, node.size * 16 / 24, node.size * 16 / 24);
    }


    @OnlyIn(Dist.CLIENT)
    protected void drawChildLines(GuiGraphics graphics, CompassNode node) {
        for (var childNode : node.getChildNodes()) {
            if (childNode.section != node.section) continue;
            var from = new Vec2(node.getPosition().x, node.getPosition().y);
            var to = new Vec2(childNode.getPosition().x, childNode.getPosition().y);

            float time = Math.abs((System.currentTimeMillis() + (node.hashCode() % 24000) + (childNode.hashCode() % 24000)) % 2400000) / 500f;

            Tesselator tesselator = Tesselator.getInstance();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(Shaders::getCompassLineShader);
            BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX_COLOR);
            RenderSystem.getShader().safeGetUniform("iTime").set(time + (childNode.hashCode() % 4000) / 1000f);
            if (editMode && mode == Mode.LINK && selectedLink != null && (selectedLink.left() == node && selectedLink.right() == childNode ||
                    selectedLink.left() == childNode && selectedLink.right() == node)) {
                RenderBufferUtils.drawColorTexLines(graphics.pose(), bufferbuilder, List.of(from, to), ColorPattern.RED.color, ColorPattern.RED.color, 16);
            } else {
                RenderBufferUtils.drawColorTexLines(graphics.pose(), bufferbuilder, List.of(from, to), 0xff57d39e, 0xff4444AA, 16);
            }
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
            RenderSystem.defaultBlendFunc();
        }
    }

}
