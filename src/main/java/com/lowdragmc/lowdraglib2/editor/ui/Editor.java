package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.menu.FileMenu;
import com.lowdragmc.lowdraglib2.editor.ui.menu.ViewMenu;
import com.lowdragmc.lowdraglib2.editor.ui.util.SplitView;
import com.lowdragmc.lowdraglib2.editor.ui.view.InspectorView;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceView;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NbtIo;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.function.Function;

@Getter
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Editor extends UIElement {
    public final UIElement top;
    public final UIElement icon;
    public final UIElement menuContainer;
    public final FileMenu fileMenu;
    public final ViewMenu viewMenu;

    public final Window left;
    public final Window right;
    public final Window center;
    public final Window bottom;

    public final InspectorView inspectorView;
    public final ResourceView resourceView;

    // runtime
    @Getter
    @Nullable
    private IProject currentProject;
    @Getter
    @Nullable
    protected File currentProjectFile;

    public Editor() {
        this.top = new UIElement();
        this.icon = new UIElement();
        this.menuContainer = new UIElement();
        this.resourceView = new ResourceView(this);

        this.left = new Window();
        this.right = new Window();
        this.center = new Window();
        this.bottom = new Window();
        this.fileMenu = new FileMenu(this);
        this.viewMenu = new ViewMenu(this);
        inspectorView = new InspectorView();

        left.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
        center.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
        right.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
        bottom.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });

        addChildren(top.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 1);
            layout.setWidthPercent(100);
            layout.setHeight(15);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 2);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID)).addChildren(icon.layout(layout -> {
            layout.setWidth(11);
            layout.setHeight(11);
            layout.setMargin(YogaEdge.ALL, 1);
            layout.setMargin(YogaEdge.HORIZONTAL, 5);
        }).style(style -> style.backgroundTexture(new SpriteTexture())), menuContainer.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 2);
        })), new SplitView.Horizontal()
                .left(new SplitView.Vertical()
                        .top(new SplitView.Horizontal()
                                .left(left)
                                .right(center)
                                .setPercentage(28)
                                .setMinPercentage(1)
                                .setMaxPercentage(99))
                        .bottom(bottom)
                        .setPercentage(75)
                        .setMinPercentage(1)
                        .setMaxPercentage(99))
                .right(right)
                .setPercentage(80)
                .setMinPercentage(1)
                .setMaxPercentage(99));

        ///  internal components
        initMenus();
        initLeftWindow();
        initRightWindow();
        initBottomWindow();
        initCenterWindow();
    }

    /**
     * Initialize the menus here.
     */
    protected void initMenus() {
        menuContainer.addChildren(fileMenu.createMenuTab(), viewMenu.createMenuTab());
    }

    protected void initLeftWindow() {

    }

    protected void initRightWindow() {
        right.addView(inspectorView);
    }

    protected void initBottomWindow() {
        bottom.addView(resourceView);
    }

    protected void initCenterWindow() {
    }

    public <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, Function<T, UIElement> uiProvider) {
        var menu = new Menu<>(menuNode, uiProvider);
        menu.layout(layout -> {
            layout.setPosition(YogaEdge.LEFT, posX - getPositionX());
            layout.setPosition(YogaEdge.TOP, posY - getContentY());
        });
        addChildren(menu);
        return menu;
    }

    public void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder) {
        if (menuBuilder == null || menuBuilder.isEmpty()) return;
        openMenu(posX, posY, menuBuilder.build(), TreeBuilder.Menu::uiProvider)
                .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                .setOnNodeClicked(TreeBuilder.Menu::handle);
    }

    public void close() {
        askToSaveProject(() -> {
            if (getModularUI() != null && getModularUI().getScreen() != null) {
                getModularUI().getScreen().onClose();
            }
        });

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
        var data = currentProject.serializeNBT(Platform.getFrozenRegistry());
        try {
            var fileData = NbtIo.read(currentProjectFile.toPath());
            return !data.equals(fileData);
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
            Dialog.showCheckBox("ldlib.gui.editor.tips.save_project", "ldlib.gui.editor.tips.ask_to_save", doSave -> {
                if (doSave) {
                    saveProject(onFinish);
                }
                if (onFinish != null) {
                    onFinish.run();
                }
            }).show(this);
            return;
        }
        if (onFinish != null) {
            onFinish.run();
        }
    }

    /**
     * Save the current project to its file if it exists, or prompt to save as if it does not.
     * @param onFinish Runnable to run after the save operation is complete, regardless of whether it was successful or not.
     */
    public void saveProject(@Nullable Runnable onFinish) {
        if (currentProject != null) {
            if (currentProjectFile == null) {
                saveAsProject(onFinish);
            } else {
                try {
                    var fileData = currentProject.serializeNBT(Platform.getFrozenRegistry());
                    NbtIo.write(fileData, currentProjectFile.toPath());
                } catch (Exception ignored) {}
                Dialog.showNotification("ldlib.gui.editor.menu.save", "ldlib.gui.compass.save_success", onFinish)
                        .show(this);
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
                                var fileData = currentProject.serializeNBT(Platform.getFrozenRegistry());
                                NbtIo.write(fileData, file.toPath());
                                currentProjectFile = file;
                            } catch (Exception ignored) {}
                        }
                        if (onFinish != null) {
                            onFinish.run();
                        }
                    }).show(this);
        }
    }

    /**
     * Load a project into the editor.
     */
    public void loadProject(IProject project, @Nullable File projectFile) {
        if (currentProject != null) {
            closeCurrentProject(true);
        }
        currentProject = project;
        currentProjectFile = projectFile;
        // load project resource
        resourceView.addResources(project.getResources());
        project.onLoad(this);
    }

    /**
     * Close the current project and clear the views.
     */
    public void closeCurrentProject(boolean checkSave) {
        inspectorView.clear();
        resourceView.clear();
        if (currentProject != null) {
            if (checkSave) {
                askToSaveProject(null);
            }
            currentProject.onClosed(this);
            currentProject = null;
            currentProjectFile = null;
        }
    }

}
