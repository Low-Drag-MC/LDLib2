package com.lowdragmc.lowdraglib2.editor.ui.menu;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.settings.BehaviorSettings;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorProjectStore;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.io.File;
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
        appendRecentProjects(menu);
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

    /**
     * The recently opened projects, as a branch that opens one straight away. Projects whose file is
     * gone are already left out by the store, and the branch is skipped entirely when none remain.
     */
    protected void appendRecentProjects(TreeBuilder.Menu menu) {
        var limit = BehaviorSettings.of(editor).getRecentProjectCount();
        if (limit <= 0) return;
        var recent = EditorProjectStore.getRecentProjects().stream().limit(limit).toList();
        if (recent.isEmpty()) return;
        menu.branch(Icons.HISTORY, "ldlib.gui.editor.menu.recent_projects", branch -> {
            for (var file : recent) {
                var type = getProjectType(file);
                // the folder is appended because several projects may well share a file name
                var parent = file.getParentFile();
                var label = Component.literal(file.getName());
                if (parent != null) {
                    label.append(Component.literal(" (" + parent.getName() + ")")
                            .withColor(ColorPattern.GRAY.color));
                }
                branch.leaf(type == null ? Icons.FILE : type.icon, label, () -> openProject(file));
            }
            branch.crossLine();
            branch.leaf(Icons.REMOVE, "ldlib.gui.editor.menu.recent_projects.clear",
                    EditorProjectStore::clearRecentProjects);
        });
    }

    /**
     * The project type that can open the given file, out of the ones registered on this menu.
     *
     * @return the matching type, or null if no registered type recognises the file.
     */
    @Nullable
    public ProjectType getProjectType(@Nullable File file) {
        if (file == null) return null;
        // the name is matched before the file system is touched, this runs per entry of a file listing
        var name = file.getName();
        var type = projectTypes.stream()
                .filter(candidate -> name.endsWith(candidate.getSuffix()))
                .findFirst()
                .orElse(null);
        return type != null && file.isFile() ? type : null;
    }

    /**
     * Loads a project file into the editor, exactly as the {@code open} entry of this menu does, prompt
     * about the currently open project included.
     *
     * @return false if the file is not a project of any registered type, so the caller can fall back.
     */
    public boolean openProject(File file) {
        var type = getProjectType(file);
        if (type == null) return false;
        try {
            editor.loadProject(type.loadProjectFromFile(file), file);
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to load the project {}: ", file, e);
            Dialog.showNotification("editor.error", "editor.loading_failed", null).show(editor);
        }
        return true;
    }

    protected void onOpenProject() {
        var suffixes = projectTypes.stream().map(ProjectType::getSuffix).toArray(String[]::new);
        Dialog.showFileDialog("ldlib.gui.editor.tips.load_project", LDLib2.getAssetsDir(), true,
                Dialog.suffixFilter(suffixes), r -> {
                    if (r != null) {
                        openProject(r);
                    }
                }).show(editor);
    }

}
