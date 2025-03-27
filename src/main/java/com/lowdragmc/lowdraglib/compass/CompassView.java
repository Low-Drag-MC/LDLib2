package com.lowdragmc.lowdraglib.compass;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;
import com.lowdragmc.lowdraglib.utils.XmlUtils;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.io.input.ReaderInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static com.lowdragmc.lowdraglib.compass.CompassManager.*;

/**
 * @author KilaBash
 * @date 2023/7/26
 * @implNote CompassView
 */
public class CompassView extends WidgetGroup {
    public static final int LIST_WIDTH = 150;

    public final String modID;
    public final ICompassUIConfig config;

    @Getter
    protected final Map<ResourceLocation, CompassSection> sections = new LinkedHashMap<>();
    protected boolean editMode = false;
    protected WidgetGroup editModeWidget;
    protected DraggableScrollableWidgetGroup sectionList;
    protected WidgetGroup mainView;
    //runtime
    @Nullable
    protected ResourceLocation openedSection, openedNode;
    @Nullable
    protected CompassSection selectedSection;
    protected Set<CompassSection> removedSections = new HashSet<>();

    public CompassView(String modID) {
        super(0, 0, 10, 10);
        setClientSideWidget();
        this.modID = modID;
        if (LDLib.isClient()) {
            this.config = INSTANCE.getUIConfig(modID);
        } else {
            this.config = null;
        }
    }

    public CompassView(CompassNode compassNode) {
        this(compassNode.section.sectionName.getNamespace());
        if (LDLib.isClient()) {
            this.openedSection = compassNode.section.getSectionName();
            this.openedNode = compassNode.getNodeName();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onScreenSizeUpdate(int screenWidth, int screenHeight) {
        setSize(Size.of(screenWidth, screenHeight));
        this.clearAllWidgets();
        this.selectedSection = null;
        this.editMode = false;
        super.onScreenSizeUpdate(screenWidth, screenHeight);
        this.addWidget(new WidgetGroup(0, INSTANCE.devMode ? 20 : 0, LIST_WIDTH, INSTANCE.devMode ? (screenHeight - 20) : screenHeight).setBackground(config.getListViewBackground()));
        addWidget(mainView = new WidgetGroup(LIST_WIDTH, 0, screenWidth - LIST_WIDTH, screenHeight));
        initCompass();
        sectionList = new DraggableScrollableWidgetGroup(4, INSTANCE.devMode ? 24 : 4, LIST_WIDTH - 8, screenHeight - (INSTANCE.devMode ? 28 : 8));
        sectionList.setYScrollBarWidth(2).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1));
        this.addWidget(sectionList);
        SelectableWidgetGroup selected = null;
        for (var section : this.sections.values()) {
            var selectable = createSelectable(section, sectionList.getAllWidgetSize() * 20);
            sectionList.addWidget(selectable);
            if (section.sectionName.equals(openedSection)) {
                selected = selectable;
            }
        }
        if (selected != null) {
            sectionList.setSelected(selected);
        }

        if (openedSection != null) {
            var section = sections.get(openedSection);
            if (section != null) {
                selectedSection = section;
                var node = section.getNode(openedNode);
                if (node != null) {
                    openNodeContent(node);
                } else {
                    openSection(section);
                }
            }
        }

        // dev mode
        if (INSTANCE.devMode) {
            this.addWidget(new SwitchWidget(0, 0, 20, 20, (cd, isPressed) -> setEditMode(isPressed)).setSupplier(() -> editMode)
                    .setTexture(new GuiTextureGroup(ColorPattern.T_GRAY.rectTexture(), Icons.EDIT_OFF),
                            new GuiTextureGroup(ColorPattern.T_CYAN.rectTexture(), Icons.EDIT_ON))
                    .setHoverTooltips(Component.translatable("ldlib.gui.compass.edit_mode")));

            this.addWidget(editModeWidget = new WidgetGroup(20, 0, LIST_WIDTH - 20, 20));
            editModeWidget.setVisible(false);
            editModeWidget.setActive(false);
            editModeWidget.addWidget(new ButtonWidget(0, 0, 20, 20,
                    new GuiTextureGroup(ColorPattern.T_RED.rectTexture(), Icons.SAVE), cd -> saveSections())
                    .setHoverTooltips(Component.translatable("ldlib.gui.editor.menu.save")));
            editModeWidget.addWidget(new ImageWidget(20, 0, LIST_WIDTH - 40, 20, new TextTexture("ldlib.gui.compass.section.edit_mode.tooltip")
                    .setWidth(LIST_WIDTH - 40)
                    .setType(TextTexture.TextType.ROLL)));
        }
    }

    @NotNull
    private SelectableWidgetGroup createSelectable(CompassSection section, int y) {
        var selectable = new SelectableWidgetGroup(0, y, LIST_WIDTH - 10, 20);
        selectable.setPrefab(section);
        selectable.setBackground(config.getListItemBackground());
        selectable.setOnSelected(s -> {
            s.setBackground(config.getListItemSelectedBackground());
            if (this.openedSection == null || !mainView.widgets.isEmpty() || !this.openedSection.equals(section.sectionName)) {
                this.openSection(section);
            }
        });
        selectable.setOnUnSelected(s -> {
            s.setBackground(config.getListItemBackground());
        });
        selectable.addWidget(new ImageWidget(2, 2, 16, 16, () -> new GuiTextureGroup(section.getButtonTexture()).scale(0.9f)));
        selectable.addWidget(new ImageWidget(22, 0, LIST_WIDTH - 35, 20, new TextTexture(section.sectionName.toLanguageKey("compass.section")).setWidth(LIST_WIDTH - 35).setType(TextTexture.TextType.LEFT_HIDE)));
        return selectable;
    }

    protected void saveSections() {
        var path = new File(LDLib.getLDLibDir(), "assets/%s/compass/sections".formatted(modID));
        if (!path.isDirectory()) {
            if (path.mkdirs()) {
                LDLib.LOGGER.info("Created directory %s".formatted(path));
            } else {
                LDLib.LOGGER.error("Failed to create directory %s".formatted(path));
                return;
            }
        }

        for (var removedSection : removedSections) {
            var file = new File(path, removedSection.getSectionName().getPath() + ".json");
            if (file.exists()) {
                if (!file.delete()) {
                    LDLib.LOGGER.error("Failed to delete file %s".formatted(file));
                }
            }
        }
        removedSections.clear();

        sectionList.widgets.stream()
                .map(SelectableWidgetGroup.class::cast)
                .sorted(Comparator.comparingInt(a -> a.getSelfPosition().y))
                .forEach(selectable -> {
                    CompassSection section = selectable.getPrefab();
                    section.priority = selectable.getSelfPosition().y / 20;
                });

        for (var section : sections.values()) {
            var file = new File(path, section.getSectionName().getPath() + ".json");
            FileUtility.saveJson(file, section.updateJson());
        }

        DialogWidget.showCheckBox(this, "ldlib.gui.compass.save_success", "ldlib.gui.compass.save_success.desc", shouldOpen -> {
            if (shouldOpen) {
                Util.getPlatform().openFile(path);
            }
        });

        CompassManager.INSTANCE.onResourceManagerReload(Minecraft.getInstance().getResourceManager());
    }

    protected void setEditMode(boolean editMode) {
        this.editMode = editMode;
        this.removedSections.clear();
        editModeWidget.setVisible(editMode);
        editModeWidget.setActive(editMode);
    }

    protected TreeBuilder.Menu createMenu() {
        var menu = TreeBuilder.Menu.start();
        menu.leaf(Icons.ADD, "ldlib.gui.compass.add_section", () -> {
            var newId = 0;
            while (sections.containsKey(ResourceLocation.parse("%s:new_section_%d".formatted(modID, newId)))) {
                newId++;
            }
            var id = ResourceLocation.parse("%s:new_section_%d".formatted(modID, newId));
            DialogWidget.showStringEditorDialog(this, "ldlib.gui.editor.tips.add_section", id.getPath(),
                    s -> LDLib.isValidResourceLocation(s) && !sections.containsKey(ResourceLocation.fromNamespaceAndPath(modID, ResourceLocation.parse(s).getPath())), s -> {
                        if (s == null || !LDLib.isValidResourceLocation(s) || sections.containsKey(ResourceLocation.fromNamespaceAndPath(modID, ResourceLocation.parse(s).getPath()))) return;
                        var config = LDLib.GSON.fromJson("""
                                        {
                                          "button_texture": {
                                            "type": "resource",
                                            "res": "ldlib:textures/gui/icon.png"
                                          }
                                        }
                                        """, JsonObject.class);
                        var section = new CompassSection(ResourceLocation.fromNamespaceAndPath(modID, ResourceLocation.parse(s).getPath()), config);
                        sections.put(section.getSectionName(), section);
                        int y = sectionList.getAllWidgetSize() * 20;
                        if (selectedSection != null) {
                            var r = sectionList.widgets.stream()
                                    .map(SelectableWidgetGroup.class::cast)
                                    .filter(selectable -> selectable.getPrefab() == selectedSection)
                                    .findFirst();
                            if (r.isPresent()) {
                                y = r.get().getSelfPosition().y + 20;
                            }
                        }
                        adjustOrder(y, false);
                        var selectable = createSelectable(section, y);
                        sectionList.addWidget(selectable);
                    });
        });
        if (selectedSection != null) {
            menu.leaf(Icons.REMOVE, "ldlib.gui.compass.remove_section", () -> {
                        if (selectedSection != null) {
                            SelectableWidgetGroup group = null;
                            for (Widget widget : sectionList.widgets) {
                                if (widget instanceof SelectableWidgetGroup selectable) {
                                    if (selectable.getPrefab() == selectedSection) {
                                        group = selectable;
                                        break;
                                    }
                                }
                            }
                            if (group != null) {
                                sectionList.removeWidget(null);
                                adjustOrder(group.getSelfPosition().y, true);
                            }
                            removedSections.add(selectedSection);
                            mainView.clearAllWidgets();
                            openedNode = null;
                            selectedSection = null;
                        }
                    })
                    .leaf("ldlib.gui.editor.menu.rename", () -> {
                        if (selectedSection != null) {
                            DialogWidget.showStringEditorDialog(this, "ldlib.gui.editor.tips.rename", selectedSection.getSectionName().getPath(),
                                    ResourceLocation::isValidPath, s -> {
                                        if (s == null || !LDLib.isValidResourceLocation(s)) return;
                                        selectedSection.setSectionName(ResourceLocation.fromNamespaceAndPath(modID, ResourceLocation.parse(s).getPath()));
                                    });
                        }
                    });
            menu.leaf(Icons.UP, "ldlib.gui.editor.menu.move_up", () -> {
                if (selectedSection != null) {
                    SelectableWidgetGroup group = null;
                    for (Widget widget : sectionList.widgets) {
                        if (widget instanceof SelectableWidgetGroup selectable) {
                            if (selectable.getPrefab() == selectedSection) {
                                group = selectable;
                                break;
                            }
                        }
                    }
                    if (group != null) {
                        var y = group.getSelfPosition().y;
                        for (Widget widget : sectionList.widgets) {
                            if (widget instanceof SelectableWidgetGroup selectable) {
                                if (selectable.getSelfPosition().y == y - 20) {
                                    selectable.setSelfPosition(Position.of(selectable.getSelfPosition().x, y));
                                    group.setSelfPosition(Position.of(group.getSelfPosition().x, y - 20));
                                }
                            }
                        }
                    }
                }
            });
            menu.leaf(Icons.DOWN, "ldlib.gui.editor.menu.move_down", () -> {
                if (selectedSection != null) {
                    SelectableWidgetGroup group = null;
                    for (Widget widget : sectionList.widgets) {
                        if (widget instanceof SelectableWidgetGroup selectable) {
                            if (selectable.getPrefab() == selectedSection) {
                                group = selectable;
                                break;
                            }
                        }
                    }
                    if (group != null) {
                        var y = group.getSelfPosition().y;
                        for (Widget widget : sectionList.widgets) {
                            if (widget instanceof SelectableWidgetGroup selectable) {
                                if (selectable.getSelfPosition().y == y + 20) {
                                    selectable.setSelfPosition(Position.of(selectable.getSelfPosition().x, y));
                                    group.setSelfPosition(Position.of(group.getSelfPosition().x, y + 20));
                                }
                            }
                        }
                    }
                }
            });
            createTextureMenu(menu, "ldlib.gui.compass.set_button_texture",
                    selectedSection.getButtonTexture(),
                    new ResourceTexture(),
                    texture -> selectedSection.setButtonTexture(texture));
            createTextureMenu(menu, "ldlib.gui.compass.set_background",
                    Optional.ofNullable(selectedSection.getBackgroundTexture()).orElse(config.getSectionBackground()),
                    config.getSectionBackground(),
                    texture -> selectedSection.setBackgroundTexture(texture == config.getSectionBackground() ? null : texture));
        }
        return menu;
    }

    protected void createTextureMenu(TreeBuilder.Menu menu, String title, IGuiTexture initial, IGuiTexture defaultTexture, Consumer<IGuiTexture> consumer) {
        menu.branch(initial, title, m -> m
                .leaf(defaultTexture == initial ? defaultTexture : IGuiTexture.EMPTY, "ldlib.gui.compass.default", () -> consumer.accept(defaultTexture))
                .leaf((defaultTexture != initial && initial instanceof ResourceTexture) ? initial : IGuiTexture.EMPTY, "ldlib:gui_texture.resource_texture", () -> DialogWidget.showStringEditorDialog(this, title, "ldlib:textures/gui/icon.png", ResourceLocation::isValidPath, s -> {
                    if (s != null && LDLib.isValidResourceLocation(s)) {
                        consumer.accept(new ResourceTexture(s));
                    }
                }))
                .leaf((defaultTexture != initial && initial instanceof ItemStackTexture) ? initial : IGuiTexture.EMPTY, "ldlib:gui_texture.item_texture", () -> DialogWidget.showItemSelector(this, title, ItemStack.EMPTY, item -> {
                    if (item != null && item != Items.AIR) {
                        consumer.accept(new ItemStackTexture(item));
                    }
                }))
                .leaf((defaultTexture != initial && initial instanceof ShaderTexture) ? initial : IGuiTexture.EMPTY, "ldlib:gui_texture.shader_texture", () -> DialogWidget.showStringEditorDialog(this, title, "ldlib:compass_node", ResourceLocation::isValidPath, s -> {
                    if (s != null && LDLib.isValidResourceLocation(s)) {
                        consumer.accept(ShaderTexture.createShader(ResourceLocation.parse(s)));
                    }
                })));
    }

    private void adjustOrder(int y, boolean moveUp) {
        for (Widget widget : sectionList.widgets) {
            if (widget instanceof SelectableWidgetGroup selectable) {
                if (selectable.getSelfPosition().y >= y) {
                    selectable.setSelfPosition(Position.of(selectable.getSelfPosition().x, selectable.getSelfPosition().y - (moveUp ? 20 : -20)));
                }
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editMode && sectionList.isMouseOverElement(mouseX, mouseY) && button == 1) {
            // menu
            this.waitToAdded(new MenuWidget<>((int) mouseX, (int) mouseY, 14, createMenu().build())
                    .setNodeTexture(MenuWidget.NODE_TEXTURE)
                    .setLeafTexture(MenuWidget.LEAF_TEXTURE)
                    .setNodeHoverTexture(MenuWidget.NODE_HOVER_TEXTURE)
                    .setCrossLinePredicate(TreeBuilder.Menu::isCrossLine)
                    .setKeyIconSupplier(TreeBuilder.Menu::getIcon)
                    .setKeyNameSupplier(TreeBuilder.Menu::getName)
                    .setOnNodeClicked(TreeBuilder.Menu::handle)
                    .setBackground(MenuWidget.BACKGROUND));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @OnlyIn(Dist.CLIENT)
    public void openSection(CompassSection section) {
        selectedSection = section;
        openedSection = section.sectionName;
        openedNode = null;
        mainView.clearAllWidgets();
        var sectionWidget = new CompassSectionWidget(this, section);
        mainView.addWidget(sectionWidget);
    }

    @OnlyIn(Dist.CLIENT)
    public void openNodeContent(CompassNode node) {
        openedNode = node.getNodeName();
        openedSection = node.getSection().getSectionName();
        mainView.clearAllWidgets();
        // page
        var pageWidget = new LayoutPageWidget(mainView.getSize().width, mainView.getSize().height);
        if (INSTANCE.devMode) { // always reload page in dev mode
            INSTANCE.nodePages.clear();
        }
        var map = INSTANCE.nodePages.computeIfAbsent(node.getPage(), x -> new HashMap<>());
        var lang = Minecraft.getInstance().getLanguageManager().getSelected();
        var document = map.computeIfAbsent(lang, langKey -> {
            var pageLocation = node.getPage();
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var path = "compass/pages/%s/%s.xml".formatted(langKey,pageLocation.getPath());
            var option = resourceManager.getResource(ResourceLocation.fromNamespaceAndPath(pageLocation.getNamespace(), path));
            if (option.isEmpty()) {
                path = "compass/pages/en_us/%s.xml".formatted(pageLocation.getPath());
                option = resourceManager.getResource(ResourceLocation.fromNamespaceAndPath(pageLocation.getNamespace(), path));
            }
            var resource = option.orElseGet(() -> resourceManager.getResource(LDLib.location("compass/pages/en_us/missing.xml")).orElseThrow());
            String content;
            try (var inputStream = resource.open()) {
                content = FileUtility.readInputStream(inputStream);
            } catch (Exception e) {
                LDLib.LOGGER.error("loading compass page {} failed", node.getPage(), e);
                content = """
                    <page>
                        <text>
                            loading page error
                        </text>
                    </page>
                    """;
            }
            try (var stream = new ReaderInputStream(new StringReader(content), StandardCharsets.UTF_8)) {
                return XmlUtils.loadXml(stream);
            } catch (Exception e) {
                LDLib.LOGGER.error("loading compass page {} failed", pageLocation, e);
            }
            return null;
        });

        if (document != null) {
            NodeList nodeList = document.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                var xmlNode = nodeList.item(i);
                if (xmlNode instanceof Element element) {
                    ILayoutComponent component = CompassManager.INSTANCE.createComponent(element.getTagName(), element);
                    if (component != null) {
                        component.createWidgets(pageWidget);
                    }
                }
            }
        }
        mainView.addWidget(pageWidget);

        // nodes
        List<CompassNode> relatedNodes = new ArrayList<>();
        relatedNodes.addAll(node.getPreNodes());
        relatedNodes.addAll(node.getChildNodes());
        var height = relatedNodes.size() * 20;
        var listHeight = Math.min(140, height);
        if (height > listHeight) {
            listHeight += 10;
        }
        var nodeList = new DraggableScrollableWidgetGroup(mainView.getSize().width - 21, (mainView.getSize().height - listHeight) / 2, 20, listHeight);
        nodeList.setBackground(new GuiTextureGroup(ColorPattern.BLACK.rectTexture(), ColorPattern.WHITE.borderTexture(1)));
        for (var compassNode : relatedNodes) {
            nodeList.addWidget(new ButtonWidget(2, 2 + nodeList.getAllWidgetSize() * 20, 16, 16,
                    compassNode.getButtonTexture(), cd -> openNodeContent(compassNode))
                    .setHoverTexture(new GuiTextureGroup(compassNode.getButtonTexture(), ColorPattern.T_GRAY.rectTexture()))
                    .setHoverTooltips(compassNode.getChatComponent()));
        }
        mainView.addWidget(nodeList);

        // buttons
        if (INSTANCE.devMode) {
            // Reset View
            mainView.addWidget(new ButtonWidget(10, 10, 20, 20,
                    new GuiTextureGroup(ColorPattern.T_GRAY.rectTexture(), Icons.ROTATION),
                    cd -> openNodeContent(node)).setHoverTooltips(Component.translatable("ldlib.gui.compass.refresh")));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void initCompass() {
        if (INSTANCE.devMode) {
            INSTANCE.onResourceManagerReload(Minecraft.getInstance().getResourceManager());
        }
        sections.clear();
        var sectionList = INSTANCE.sections.getOrDefault(modID, Collections.emptyMap());
        sectionList.values().stream().sorted(Comparator.comparingInt(a -> a.priority)).forEach(section -> {
            sections.put(section.sectionName, section);
        });
    }

}
