package com.lowdragmc.lowdraglib2.editor.ui;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.settings.AppearanceSettings;
import com.lowdragmc.lowdraglib2.editor.settings.BehaviorSettings;
import com.lowdragmc.lowdraglib2.editor.settings.EditorSettings;
import com.lowdragmc.lowdraglib2.editor.ui.menu.FileMenu;
import com.lowdragmc.lowdraglib2.editor.ui.menu.ViewMenu;
import com.lowdragmc.lowdraglib2.editor.ui.view.HistoryView;
import com.lowdragmc.lowdraglib2.editor.ui.view.InspectorView;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceView;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import lombok.Getter;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.List;

@Getter
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class Editor extends UIElement {
    public final UIElement top;
    public final UIElement icon;
    public final UIElement menuContainer;
    public final UIElement topPlaceholder;
    public final UIElement buttonContainer;

    public final Button closeButton;

    public final FileMenu fileMenu;
    public final ViewMenu viewMenu;

    public final UIElement mainView;

    public final SplittableWindow rootWindow;
    public final SplittableWindow leftWindow;
    public final SplittableWindow rightWindow;
    public final SplittableWindow centerWindow;
    public final SplittableWindow bottomWindow;

    public final InspectorView inspectorView;
    public final ResourceView resourceView;
    public final HistoryView historyView;

    public final EditorSettings editorSettings;

    // runtime
    @Getter
    @Nullable
    private EditorWindow window;
    @Getter
    @Nullable
    private IProject currentProject;
    @Getter
    @Nullable
    protected File currentProjectFile;

    public Editor() {
        getLayout().widthPercent(100);
        getLayout().heightPercent(100);

        addClass("__editor__");

        // top bar
        this.top = new UIElement();
        this.icon = new UIElement();
        this.menuContainer = new UIElement();
        this.topPlaceholder = new UIElement();
        this.buttonContainer = new UIElement();

        this.closeButton = new Button();

        // view
        this.historyView = new HistoryView(this);
        this.inspectorView = new InspectorView(this);
        this.resourceView = new ResourceView(this);

        // view container
        this.mainView = new UIElement();

        // menu
        this.fileMenu = new FileMenu(this);
        this.viewMenu = new ViewMenu(this);

        this.editorSettings = createSettings();

        rootWindow = new SplittableWindow().setImmortal(true);
        var split1 = rootWindow
                .splitStyle(style -> style.percentage(80).minPercentage(5).maxPercentage(95))
                .splitNew(SplittableWindow.Edge.LEFT);
        rightWindow = split1.getSecond().setImmortal(true);
        var split2 = split1.getFirst()
                .splitStyle(style -> style.percentage(75).minPercentage(5).maxPercentage(95))
                .splitNew(SplittableWindow.Edge.TOP);
        bottomWindow = split2.getSecond().setImmortal(true);
        var split3 = split2.getFirst()
                .splitStyle(style -> style.percentage(28).minPercentage(5).maxPercentage(95))
                .splitNew(SplittableWindow.Edge.LEFT);
        centerWindow = split3.getSecond().setImmortal(true);
        leftWindow = split3.getFirst().setImmortal(true);

        addChildren(
                top.layout(layout -> {
                    layout.paddingAll(1);
                    layout.widthPercent(100);
                    layout.height(15);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(2);
                }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID))
                .addChildren(
                        icon.layout(layout -> {
                            layout.width(11);
                            layout.height(11);
                            layout.marginAll(1);
                            layout.marginHorizontal(5);
                        }).style(style -> style.backgroundTexture(new SpriteTexture())),
                        menuContainer.layout(layout -> {
                            layout.heightPercent(100);
                            layout.flexDirection(FlexDirection.ROW);
                            layout.gapAll(2);
                        }).addClass("__editor_top-menu-container__"),
                        topPlaceholder.layout(layout -> layout.flex(1))
                                .addClass("__editor_top-placeholder__"), // placeholder
                        buttonContainer.layout(layout -> {
                            layout.flexDirection(FlexDirection.ROW);
                            layout.alignItems(AlignItems.CENTER);
                            layout.gapAll(2);
                            layout.marginRight(1);
                        }).addChildren(
                                closeButton.noText().addPreIcon(Icons.WINDOW_CLOSE).layout(layout -> layout.height(12))
                        ).addClass("__editor_top_button-container__")
                ),
                mainView.layout(layout -> {
                    layout.widthPercent(100);
                    layout.flex(1);
                }).addChild(rootWindow)
        );

        closeButton.setOnClick(e -> close());

        top.addClass("__editor_top__").moveInlineAsDefault();
        mainView.addClass("__editor_main__").moveInlineAsDefault();

        /// internal components
        initEditorSettings();
        editorSettings.loadAllSettingsFromFile();
        editorSettings.applyCurrentSettings();

        initMenus();
        onPrepareInspectorView();
        onPrepareHistoryView();
        onPrepareResourceView();

        /// events
        addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);
    }

    /**
     * Used to create seperated editor.
     */
    protected abstract Editor createNewEditorInstance();

    protected void _setEditorWindowInternal(@Nullable EditorWindow window) {
        this.window = window;
    }

    /**
     * Initialize the menus here.
     */
    protected void initMenus() {
        menuContainer.addChildren(fileMenu.createMenuTab(), viewMenu.createMenuTab());
    }

    protected EditorSettings createSettings() {
        return new EditorSettings(this);
    }

    protected void initEditorSettings() {
        if (!LDLib2.isClient()) return;
        editorSettings.registerSettings(new AppearanceSettings(), AppearanceSettings.CODEC);
        editorSettings.registerSettings(new BehaviorSettings(), BehaviorSettings.CODEC);
    }

    protected void onPrepareInspectorView() {
        rightWindow.getRightTop().addView(inspectorView);
    }

    protected void onPrepareHistoryView() {
        rightWindow.getRightTop().addViews(historyView);
    }

    protected void onPrepareResourceView() {
        bottomWindow.getLeftBottom().addView(resourceView);
    }

    public Component getTitle() {
        if (currentProject == null) {
            return Component.translatable("editor.empty_editor");
        } else {
            var title = Component.translatable("editor.open_project", Component.translatable(currentProject.getName()));
            if (currentProjectFile != null) {
                title.append(" - ").append(currentProjectFile.getPath());
            }
            return title;
        }
    }

    public List<View> getAllViews() {
        return rootWindow.getAllViews();
    }

    public <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, UIElementProvider<T> uiProvider) {
        var menu = new Menu<>(menuNode, uiProvider);
        var mui = getModularUI();
        if (mui == null) {
            menu.layout(layout -> {
                layout.left(posX - getContentX());
                layout.top(posY - getContentY());
            });
            addChildren(menu);
        } else {
            menu.layout(layout -> {
                layout.left(posX - mui.ui.rootElement.getContentX());
                layout.top(posY - mui.ui.rootElement.getContentY());
            });
            mui.ui.rootElement.addChildren(menu);
        }
        return menu;
    }

    public void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder) {
        if (menuBuilder == null || menuBuilder.isEmpty()) return;
        openMenu(posX, posY, menuBuilder.build(), TreeBuilder.Menu::uiProvider)
                .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                .setOnNodeClicked(TreeBuilder.Menu::handle);
    }

    /**
     * Close the entire editor window.
     */
    public void close() {
        if (window != null) {
            window.closeWindow();
        } else {
            exit();
        }
    }

    public void exit() {
        exit(null);
    }

    /**
     * Exit the current editor and run the given runnable after the editor is closed.
     * It doesn't mean the window is closed.
     */
    public void exit(@Nullable Runnable onFinish) {
        askToSaveProject(() -> {
            if (window != null) {
                window.removeEditor(this);
            } else {
                if (getModularUI() != null && ModularUIClientAccess.getScreen(getModularUI()) != null) {
                    ModularUIClientAccess.getScreen(getModularUI()).onClose();
                }
            }
            if (onFinish != null) {
                onFinish.run();
            }
        });
    }

    public void openSettingsPanel() {
        var dialog = new Dialog();
        dialog.setAutoClose(false);
        dialog.width(TaffyDimension.length(350));
        dialog.setTitle("editor.settings");
        dialog.addContent(editorSettings.createSettingsPanel());

        var cancelButton = new Button();
        cancelButton.text.textStyle(textStyle -> textStyle.textColor(ColorPattern.GRAY.color));
        cancelButton.setActive(false);

        dialog.addButton(new Button()
                .setOnClick(e -> {
                    if (editorSettings.isDirty()) {
                        editorSettings.applyCurrentSettings();
                        editorSettings.saveAllSettingsToFile();
                    }
                    dialog.close();
                })
                .setText("ldlib.gui.tips.confirm"));
        dialog.addButton(new Button()
                .setOnClick(e -> {
                    editorSettings.restoreSettings();
                    editorSettings.applyCurrentSettings();
                    dialog.close();
                })
                .setText("ldlib.gui.tips.cancel"));
        dialog.addButton(cancelButton
                .setOnClick(e -> {
                    if (editorSettings.isDirty()) {
                        editorSettings.applyCurrentSettings();
                        editorSettings.saveAllSettingsToFile();
                    }
                })
                .setText("ldlib.gui.tips.apply"));
        dialog.addEventListener(UIEvents.TICK, e -> {
            var isDirty = editorSettings.isDirty();
            cancelButton.text.textStyle(textStyle -> textStyle.textColor(isDirty ? ColorPattern.WHITE.color : ColorPattern.GRAY.color));
            cancelButton.setActive(isDirty);
        });

        dialog.show(this.getModularUI());
    }

    /**
     * Check if the current project is dirty if the project file exists.
     * It will compare the current project serialized data with the saved file.
     */
    public boolean isCurrentProjectDirty() {
        if (currentProject == null) {
            return false; // No project loaded
        }
        if (currentProjectFile == null) {
            return true; // Project is dirty if it has not been saved yet
        }
        try {
            return currentProject.getProjectType().isProjectDirty(currentProject, currentProjectFile);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Ask the user to save the current project if it is dirty.
     * @param onFinish Runnable to run after the dialog is closed, regardless of whether the project was saved or not.
     */
    public void askToSaveProject(@Nullable Runnable onFinish) {
        if (isCurrentProjectDirty()) {
            var dialog = Dialog.showCancelableCheck("ldlib.gui.editor.tips.save_project", "ldlib.gui.editor.tips.ask_to_save", doSave -> {
                if (doSave) {
                    saveProject(onFinish);
                } else {
                    if (onFinish != null) {
                        onFinish.run();
                    }
                }
            }, Runnables.doNothing());
            dialog.titleBar.addChild(new Label()
                    .textStyle(style -> style
                            .textAlignVertical(Vertical.CENTER)
                            .textAlignHorizontal(Horizontal.CENTER)
                            .textWrap(TextWrap.HOVER_ROLL))
                    .setText(Component.literal("-").append(getTitle()))
                    .setOverflowVisible(false)
                    .layout(layout -> layout.flex(1)));
            dialog.show(this.getModularUI());
            if (dialog.buttonContainer.getChildren().getFirst() instanceof Button button) {
                button.setText("ldlib.gui.editor.menu.save");
            }
            return;
        }
        if (onFinish != null) {
            onFinish.run();
        }
    }

    public void saveProject(@Nullable Runnable onFinish) {
        saveProject(onFinish, true);
    }

    /**
     * Save the current project to its file if it exists, or prompt to save as if it does not.
     * @param onFinish Runnable to run after the save operation is complete, regardless of whether it was successful or not.
     */
    public void saveProject(@Nullable Runnable onFinish, boolean showNotification) {
        if (currentProject != null) {
            if (currentProjectFile == null) {
                saveAsProject(onFinish);
            } else {
                try {
                    currentProject.getProjectType().saveProjectToFile(currentProject, currentProjectFile);
                } catch (Exception ignored) {}
                if (showNotification) {
                    if (onFinish != null) {
                        Dialog.showNotification("ldlib.gui.editor.menu.save", "ldlib.gui.compass.save_success", onFinish)
                                .show(this.getModularUI());
                    } else {
                        Dialog.showNotification("ldlib.gui.compass.save_success", 2)
                                .show(this.getModularUI());
                    }
                }
            }
        }
    }

    /**
     * Save the current project as a new file.
     * @param onFinish Runnable to run after the save operation is complete, regardless of whether it was successful or not.
     */
    public void saveAsProject(@Nullable Runnable onFinish) {
        if (currentProject != null) {
            String suffix = currentProject.getSuffix();
            Dialog.showFileDialog("ldlib.gui.editor.tips.save_as", LDLib2.getAssetsDir(), false,
                    Dialog.suffixFilter(suffix), file -> {
                        if (file != null && !file.isDirectory()) {
                            if (!file.getName().endsWith(suffix)) {
                                file = new File(file.getParentFile(), file.getName() + suffix);
                            }
                            try {
                                currentProject.getProjectType().saveProjectToFile(currentProject, file);
                                currentProjectFile = file;
                            } catch (Exception ignored) {}
                        }
                        if (onFinish != null) {
                            onFinish.run();
                        }
                    }).show(this.getModularUI());
        }
    }

    /**
     * Load a project into the editor.
     */
    public final void loadProject(IProject project, @Nullable File projectFile) {
        if (currentProject != null) {
            if (window != null) {
                Dialog.showCheckBox("Dialog.info","editor.loadProject.info", result -> {
                   if (result) {
                       window.createNewEditor(this::createNewEditorInstance).loadNewProject(project, projectFile);
                   } else {
                       closeCurrentProject(true, () -> loadNewProject(project, projectFile));
                   }
                }).show(window);
            } else {
                closeCurrentProject(true, () -> loadNewProject(project, projectFile));
            }
        } else {
            loadNewProject(project, projectFile);
        }
    }

    protected void loadNewProject(IProject project, @Nullable File projectFile) {
        currentProject = project;
        currentProjectFile = projectFile;
        // load project resource
        resourceView.loadResources(project.getResources());
        historyView.recordSerializableObject(Component.translatable("editor.open"), currentProject);
        project.onLoad(this);
    }


    /**
     * Close the current project and clear the views.
     */
    public final void closeCurrentProject(boolean checkSave, @Nullable Runnable onFinish) {
        if (currentProject != null) {
            if (checkSave) {
                askToSaveProject(() -> {
                    closeCurrentProject();
                    if (onFinish != null) {
                        onFinish.run();
                    }
                });
            } else {
                closeCurrentProject();
                if (onFinish != null) {
                    onFinish.run();
                }
            }
        }
    }

    protected void closeCurrentProject() {
        if (currentProject != null) {
            currentProject.onClosed(this);
            currentProject = null;
            currentProjectFile = null;
        }
        inspectorView.clear();
        resourceView.clear();
        historyView.clearHistory();
    }

    protected void onValidateCommand(UIEvent event) {
        if (CommandEvents.SAVE.equals(event.command) && getCurrentProject() != null) {
            event.stopPropagation();
        }
    }

    protected void onExecuteCommand(UIEvent event) {
        if (CommandEvents.SAVE.equals(event.command) && getCurrentProject() != null) {
            if (getCurrentProjectFile() != null) {
                saveProject(null);
            } else {
                saveAsProject(null);
            }
        }
    }

    public static Editor emptyEditor() {
        return new Editor() {
            @Override
            protected Editor createNewEditorInstance() {
                return emptyEditor();
            }
        };
    }
}
