package com.lowdragmc.lowdraglib2.editor.ui.menu;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Dialog;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import lombok.Data;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class FileMenu extends MenuTab {
    @Data(staticConstructor = "of")
    public static class ProjectProvider {
        public final IGuiTexture icon;
        public final String name;
        public final String suffix;
        public final Supplier<IProject> projectCreator;
    }

    private final List<ProjectProvider> projectProviders = new ArrayList<>();
    private final List<BiConsumer<MenuTab, TreeBuilder.Menu>> newMenuCreators = new ArrayList<>();

    public FileMenu(Editor editor) {
        super(editor);
    }

    @Override
    protected TreeBuilder.Menu createDefaultMenu() {
        var menu = TreeBuilder.Menu.start();
        menu.branch("ldlib.gui.editor.menu.new", newMenu -> {
            for (var provider : projectProviders) {
                newMenu.leaf(provider.icon, provider.name, () -> {
                    // open a new project
                    var newProject = provider.projectCreator.get();
                    newProject.initNewProject();
                    editor.loadProject(newProject, null);
                });
            }
            newMenu.crossLine();
            newMenuCreators.forEach(creator -> creator.accept(this, newMenu));
        });
        menu.leaf(Icons.OPEN_FILE, "ldlib.gui.editor.menu.open", this::onOpenProject);
        menu.crossLine();
        if (editor.getCurrentProject() != null) {
            if (editor.getCurrentProjectFile() != null) {
                menu.leaf(Icons.SAVE, "ldlib.gui.editor.tips.save", () -> editor.saveProject(null));
            }
            menu.leaf(Icons.SAVE, "ldlib.gui.editor.tips.save_as", () -> editor.saveAsProject(null));
        }
        menu.crossLine();
        menu.leaf("editor.exist", editor::close);
        return menu;
    }

    @Override
    protected Component getComponent() {
        return Component.translatable("editor.file");
    }

    /**
     * Add a project provider to the file menu. It will be displayed in the {@code new} branch
     * @param projectProvider the project provider to add
     */
    public void addProjectProvider(ProjectProvider projectProvider) {
        this.projectProviders.add(projectProvider);
    }

    /**
     * Append new menu creator to attach additional leafs to the menu or remove existing ones.
     */
    public ISubscription registerNewMenuCreator(BiConsumer<MenuTab, TreeBuilder.Menu> newCreator) {
        this.newMenuCreators.add(newCreator);
        return () -> this.newMenuCreators.remove(newCreator);
    }

    protected void onOpenProject() {
        var suffixes = projectProviders.stream().map(ProjectProvider::getSuffix).toArray(String[]::new);
        Dialog.showFileDialog("ldlib.gui.editor.tips.load_project", LDLib2.getAssetsDir(), true,
                Dialog.suffixFilter(suffixes), r -> {
                    if (r != null && r.isFile()) {
                        var fileName = r.getName();
                        projectProviders.stream()
                                .filter(provider -> fileName.endsWith(provider.getSuffix()))
                                .findFirst()
                                .ifPresent(provider -> {
                                    try {
                                        var data = NbtIo.read(r.toPath());
                                        var project = provider.getProjectCreator().get();
                                        project.deserializeNBT(Platform.getFrozenRegistry(), Objects.requireNonNull(data));
                                        editor.loadProject(project, r);
                                    } catch (Exception e) {
                                        Dialog.showNotification("editor.error", "editor.loading_failed", null).show(editor);
                                    }
                                });
                    }
                }).show(editor);
    }

}
