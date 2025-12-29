package com.lowdragmc.lowdraglib2.editor.ui.menu;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class FileMenu extends MenuTab {
    private final List<ProjectType> projectTypes = new ArrayList<>();
    private final List<BiConsumer<MenuTab, TreeBuilder.Menu>> newMenuCreators = new ArrayList<>();

    public FileMenu(Editor editor) {
        super(editor);
    }

    @Override
    protected TreeBuilder.Menu createDefaultMenu() {
        var menu = TreeBuilder.Menu.start();
        menu.branch("ldlib.gui.editor.menu.new", newMenu -> {
            for (var type : projectTypes) {
                newMenu.leaf(type.icon, type.name, () -> {
                    // open a new project
                    editor.loadProject(type.newEmptyProject(), null);
                });
            }
            newMenu.crossLine();
            newMenuCreators.forEach(creator -> creator.accept(this, newMenu));
        });
        menu.leaf(Icons.OPEN_FILE, "ldlib.gui.editor.menu.open", this::onOpenProject);
        menu.crossLine();
        if (editor.getCurrentProject() != null) {
            if (editor.getCurrentProjectFile() != null) {
                menu.leaf(Icons.SAVE, "ldlib.gui.editor.tips.save.menu", () -> editor.saveProject(null));
            }
            menu.leaf(Icons.SAVE, "ldlib.gui.editor.tips.save_as.menu", () -> editor.saveAsProject(null));
        }
        menu.crossLine();
        return menu;
    }

    @Override
    protected TreeBuilder.Menu createMenu() {
        var menu = super.createMenu();
        menu.crossLine();
        menu.leaf("editor.settings.menu", editor::openSettingsPanel);
        menu.crossLine();
        menu.leaf("editor.exit", editor::exit);
        return menu;
    }

    @Override
    protected Component getComponent() {
        return Component.translatable("editor.file");
    }

    /**
     * Add a project type to the file menu. It will be displayed in the {@code new} branch
     * @param projectType the project type to add
     */
    public void addProjectProvider(ProjectType projectType) {
        this.projectTypes.add(projectType);
    }

    /**
     * Append new menu creator to attach additional leafs to the menu or remove existing ones.
     */
    public ISubscription registerNewMenuCreator(BiConsumer<MenuTab, TreeBuilder.Menu> newCreator) {
        this.newMenuCreators.add(newCreator);
        return () -> this.newMenuCreators.remove(newCreator);
    }

    protected void onOpenProject() {
        var suffixes = projectTypes.stream().map(ProjectType::getSuffix).toArray(String[]::new);
        Dialog.showFileDialog("ldlib.gui.editor.tips.load_project", LDLib2.getAssetsDir(), true,
                Dialog.suffixFilter(suffixes), r -> {
                    if (r != null && r.isFile()) {
                        var fileName = r.getName();
                        projectTypes.stream()
                                .filter(type -> fileName.endsWith(type.getSuffix()))
                                .findFirst()
                                .ifPresent(type -> {
                                    try {
                                        var project = type.loadProjectFromFile(r);
                                        editor.loadProject(project, r);
                                    } catch (Exception e) {
                                        Dialog.showNotification("editor.error", "editor.loading_failed", null).show(editor);
                                    }
                                });
                    }
                }).show(editor);
    }

}
