package com.lowdragmc.lowdraglib2.editor_outdated.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor_outdated.data.IProject;
import com.lowdragmc.lowdraglib2.editor_outdated.ui.view.HistoryView;
import com.lowdragmc.lowdraglib2.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib2.gui.widget.MenuWidget;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.nbt.NbtIo;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2022/11/30
 * @implNote MainPage
 */
@Getter
public abstract class Editor extends WidgetGroup {
    @OnlyIn(Dist.CLIENT)
    public static Editor INSTANCE;
    protected final File workSpace;
    protected IProject currentProject;
    @Setter
    @Nullable
    protected File currentProjectFile;
    protected MenuPanel menuPanel;
    protected StringTabContainer tabPages;
    protected ConfigPanel configPanel;
    protected ResourcePanel resourcePanel;
    protected WidgetGroup floatView;
    protected ToolPanel toolPanel;
    protected String copyType;
    protected Object copied;
    public record HistoryItem(String name, CompoundTag date, @Nullable Object source) { }
    protected final List<HistoryItem> history = new ArrayList<>();
    @Nullable
    protected HistoryItem currentHistory;

    public Editor(String modID) {
        this(new File(LDLib2.getAssetsDir(), modID));
        if (LDLib2.isClient()) {
            if (!this.workSpace.exists() && !this.workSpace.mkdirs()) {
                LDLib2.LOGGER.error("Failed to create work space for mod: " + modID);
            }
        }
    }

    public Editor(File workSpace) {
        super(0, 0, 10, 10);
        setClientSideWidget();
        this.workSpace = workSpace;
    }

    @Override
    public abstract String group();

    @Override
    public abstract String name();

    @Override
    public void setGui(ModularUI gui) {
        if (gui != null) {
            gui.mainGroup.setClientSideWidget();
            gui.mainGroup.setAllowXEIIngredientOverMouse(false);
        }
        super.setGui(gui);
        if (isRemote()) {
            if (gui == null) {
                INSTANCE = null;
            } else {
                INSTANCE = this;
                getGui().registerCloseListener(() -> INSTANCE = null);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onScreenSizeUpdate(int screenWidth, int screenHeight) {
        setSize(Size.of(screenWidth, screenHeight));
        super.onScreenSizeUpdate(screenWidth, screenHeight);
        this.clearAllWidgets();
        initEditorViews();
        var lastPageIndex = tabPages == null ? -1 : tabPages.getTabIndex();
        loadProject(currentProject);
        if (tabPages != null) {
            tabPages.switchTabIndex(lastPageIndex);
        }
    }

    public void initEditorViews() {
        this.toolPanel = new ToolPanel(this);
        this.configPanel = new ConfigPanel(this);
        this.tabPages = new StringTabContainer(this);
        this.resourcePanel = new ResourcePanel(this);
        this.menuPanel = new MenuPanel(this);
        this.floatView = new WidgetGroup(0, 0, this.getSize().width, this.getSize().height);

        this.addWidget(this.tabPages);
        this.addWidget(this.toolPanel);
        this.addWidget(this.configPanel);
        this.addWidget(this.resourcePanel);
        this.addWidget(this.menuPanel);
        this.addWidget(this.floatView);
    }

    public DialogWidget openDialog(DialogWidget dialog) {
        this.addWidget(dialog);
        Position pos = dialog.getPosition();
        Size size = dialog.getSize();
        if (pos.x + size.width > getGui().getScreenWidth()) {
            dialog.addSelfPosition(pos.x + size.width - getGui().getScreenWidth(), 0);
        } else if (pos.x < 0) {
            dialog.addSelfPosition(-pos.x, 0);
        }
        if (pos.y + size.height > getGui().getScreenHeight()) {
            dialog.addSelfPosition(0, pos.y + size.height - getGui().getScreenHeight());
        } else if (pos.y < 0) {
            dialog.addSelfPosition(0, -pos.y);
        }
        return dialog;
    }

    public <T, C> MenuWidget<T, C> openMenu(double posX, double posY, TreeNode<T, C> menuNode) {
        var menu = new MenuWidget<>((int) posX, (int) posY, 14, menuNode)
                .setNodeTexture(MenuWidget.NODE_TEXTURE)
                .setLeafTexture(MenuWidget.LEAF_TEXTURE)
                .setNodeHoverTexture(MenuWidget.NODE_HOVER_TEXTURE);
        waitToAdded(menu.setBackground(MenuWidget.BACKGROUND));

        return menu;
    }

    public void openMenu(double posX, double posY, TreeBuilder.Menu menuBuilder) {
        if (menuBuilder == null) return;
        openMenu(posX, posY, menuBuilder.build())
                .setCrossLinePredicate(TreeBuilder.Menu::isCrossLine)
                .setKeyIconSupplier(TreeBuilder.Menu::getIcon)
                .setKeyNameSupplier(TreeBuilder.Menu::getName)
                .setOnNodeClicked(TreeBuilder.Menu::handle);
    }

    public void addRawHistory(String name, CompoundTag date) {
        addRawHistory(name, date, null);
    }

    public void addRawHistory(String name, CompoundTag date, @Nullable Object source) {
        if (currentProject != null) {
            if (currentHistory != null) {
                var index = history.indexOf(currentHistory);
                if (index >= 0) {
                    while (history.size() > index + 1) {
                        history.remove(history.size() - 1);
                    }
                }
            }
            currentHistory = new HistoryItem(name, date, source);
            history.add(currentHistory);
            // if history view is opened, update it
            for (Widget widget : floatView.widgets) {
                if (widget instanceof HistoryView historyView) {
                    historyView.loadList();
                    break;
                }
            }
        }
    }

    public void addAutoHistory(String name, @Nullable Object source) {
        if (this.currentProject != null) {
            // update history view
            var historyName = LocalizationUtils.format(name);
            var data = this.currentProject.serializeNBT(Platform.getFrozenRegistry());
            if (!this.getHistory().isEmpty()) {
                var latestHistory = this.getHistory().get(this.getHistory().size() - 1);
                // if the data is the same as the latest history, do not add a new history
                if (!data.equals(latestHistory.date())) {
                    // if new history is the same as the latest history, update the latest history
                    if (latestHistory.name().equals(historyName) && Objects.equals(latestHistory.source(), source)) {
                        this.getHistory().remove(latestHistory);
                    }
                    this.addRawHistory(historyName, data, source);
                }
            } else {
                this.addRawHistory(historyName, data, source);
            }
        }
    }

    public void jumpToHistory(HistoryItem historyItem) {
        if (currentProject != null && history.contains(historyItem)) {
            var lastPageIndex = tabPages.getTabIndex();
            currentProject.deserializeNBT(Platform.getFrozenRegistry(), historyItem.date());
            loadProject(currentProject);
            tabPages.switchTabIndex(lastPageIndex);
            currentHistory = historyItem;
        }
    }

    public void loadProject(IProject project) {
        if (currentProject != null && currentProject != project) {
            currentProject.onClosed(this);
            currentProjectFile = null;
        }

        currentProject = project;
        tabPages.clearAllWidgets();
        toolPanel.clearAllWidgets();
        toolPanel.hide(false);
        configPanel.clearAllConfigurators();
        resourcePanel.clear();
        floatView.clearAllWidgets();

        if (currentProject != null) {
            currentProject.onLoad(this);
        }
    }

    public void setCopy(String copyType, Object copied) {
        this.copied = copied;
        this.copyType = copyType;
    }

    public void ifCopiedPresent(String copyType, Consumer<Object> consumer) {
        if (Objects.equals(copyType, this.copyType)) {
            consumer.accept(copied);
        }
    }

    public void askToSaveProject(BooleanConsumer result) {
        DialogWidget.showCheckBox(this, "ldlib.gui.editor.tips.save_project", "ldlib.gui.editor.tips.ask_to_save", isSave -> {
            if (isSave) {
                saveProject(result);
            } else {
                result.accept(false);
            }
        });
    }

    public void saveProject(BooleanConsumer result) {
        if (currentProject != null) {
            if (currentProjectFile == null) {
                saveAsProject(result);
            } else {
                currentProject.saveProject(currentProjectFile.toPath());
                DialogWidget.showNotification(this, "ldlib.gui.editor.menu.save", "ldlib.gui.compass.save_success");
                result.accept(true);
            }
        } else {
            result.accept(false);
        }
    }

    public void saveAsProject(BooleanConsumer result) {
        if (currentProject != null) {
            String suffix = "." + currentProject.getSuffix();
            DialogWidget.showFileDialog(this, "ldlib.gui.editor.tips.save_as", currentProject.getProjectWorkSpace(this), false,
                    DialogWidget.suffixFilter(suffix), file -> {
                        if (file != null && !file.isDirectory()) {
                            if (!file.getName().endsWith(suffix)) {
                                file = new File(file.getParentFile(), file.getName() + suffix);
                            }
                            currentProject.saveProject(file.toPath());
                            currentProjectFile = file;
                            result.accept(true);
                        } else {
                            result.accept(false);
                        }
                    });
        }
    }

    public boolean isCurrentProjectSaved() {
        if (currentProject == null) return true;
        if (currentProjectFile == null) return false;
        try {
            var tag = NbtIo.read(currentProjectFile.toPath());
            return tag != null && tag.equals(currentProject.serializeNBT(Platform.getFrozenRegistry()));
        } catch (IOException ignored) {}
        return false;
    }

    private boolean isWaitingForSave = false;

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (isWaitingForSave) {
                getGui().getModularUIGui().onClose();
                return true;
            }
            // esc
            if (!isCurrentProjectSaved()) {
                isWaitingForSave = true;
                askToSaveProject(result -> getGui().getModularUIGui().onClose());
                return true;
            }
            return false;
        }
        super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }
}
