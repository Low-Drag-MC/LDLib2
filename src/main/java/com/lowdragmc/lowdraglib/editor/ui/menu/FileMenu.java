package com.lowdragmc.lowdraglib.editor.ui.menu;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.editor.ui.Editor;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.registry.AutoRegistry;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import lombok.Setter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FileMenu extends MenuTab {
//    @Setter
//    protected Predicate<IProject> projectFilter = project -> project.group().startsWith(editor.name());

    public FileMenu(Editor editor) {
        super(editor);
    }

    @Override
    protected TreeBuilder.Menu createDefaultMenu() {
        var menu = TreeBuilder.Menu.start();
//                .branch("ldlib.gui.editor.menu.new", this::newProject)
//                .crossLine()
//                .leaf(Icons.OPEN_FILE, "ldlib.gui.editor.menu.open", this::openProject);
//        if (editor.getCurrentProjectFile() != null) {
//            menu.leaf(Icons.SAVE, "ldlib.gui.editor.tips.save", () -> editor.saveProject(result -> {
//            }));
//        }
//        menu.leaf(Icons.SAVE, "ldlib.gui.editor.tips.save_as", () -> editor.saveAsProject(result -> {
//                }))
//                .crossLine()
//                .branch(Icons.IMPORT, "ldlib.gui.editor.menu.import", m -> m.leaf("ldlib.gui.editor.menu.resource", this::importResource))
//                .branch(Icons.EXPORT, "ldlib.gui.editor.menu.export", m -> m.leaf("ldlib.gui.editor.menu.resource", this::exportResource));
        menu.leaf("editor.exist", editor::close);
        return menu;
    }

    @Override
    protected Component getComponent() {
        return Component.translatable("editor.file");
    }


//    private void exportResource() {
//        var resources = editor.getResourcePanel().getResources();
//        if (resources != null) {
//            DialogWidget.showFileDialog(editor, "ldlib.gui.editor.tips.save_resource", editor.getWorkSpace(), false,
//                    DialogWidget.suffixFilter(".resource"), r -> {
//                        if (r != null && !r.isDirectory()) {
//                            if (!r.getName().endsWith(".resource")) {
//                                r = new File(r.getParentFile(), r.getName() + ".resource");
//                            }
//                            try {
//                                NbtIo.write(resources.serializeNBT(Platform.getFrozenRegistry()), r.toPath());
//                            } catch (IOException exception) {
//                                LDLib.LOGGER.error("Failed to save resource", exception);
//                            }
//                        }
//                    });
//        }
//    }
//
//    private void importResource() {
//        var currentProject = editor.getCurrentProject();
//        if (currentProject != null) {
//            DialogWidget.showFileDialog(editor, "ldlib.gui.editor.tips.load_resource", editor.getWorkSpace(), true,
//                    DialogWidget.suffixFilter(".resource"), r -> {
//                        if (r != null && r.isFile()) {
//                            try {
//                                var tag = NbtIo.read(r.toPath());
//                                if (tag != null) {
//                                    editor.getResourcePanel().loadResource(currentProject.loadResources(tag), true);
//                                }
//                            } catch (IOException exception) {
//                                LDLib.LOGGER.error("Failed to load resource", exception);
//                            }
//                        }
//                    });
//        }
//    }
//
//    protected Predicate<IProject> getProjectPredicate() {
//        return projectFilter;
//    }
//
//    private void newProject(TreeBuilder.Menu menu) {
//        for (var project : LDLibRegistries.PROJECTS.values().stream()
//                .map(AutoRegistry.Holder::value).map(Supplier::get).filter(getProjectPredicate()).toList()) {
//            menu = menu.leaf(project.getTranslateKey(), () -> {
//                if (editor.isCurrentProjectSaved()) {
//                    editor.loadProject(project.newEmptyProject());
//                } else {
//                    editor.askToSaveProject(result -> {
//                        editor.loadProject(project.newEmptyProject());
//                    });
//                }
//            });
//        }
//    }
//
//    private void openProject() {
//        var suffixes = LDLibRegistries.PROJECTS.values().stream()
//                .map(AutoRegistry.Holder::value).map(Supplier::get)
//                .filter(getProjectPredicate()).map(IProject::getSuffix).collect(Collectors.toSet());
//        DialogWidget.showFileDialog(editor, "ldlib.gui.editor.tips.load_project", editor.getWorkSpace(), true,
//                node -> {
//                    if (node.isLeaf() && node.getContent().isFile()) {
//                        String file = node.getContent().getName().toLowerCase();
//                        for (String suffix : suffixes) {
//                            if (file.endsWith(suffix.toLowerCase())) {
//                                return true;
//                            }
//                        }
//                        return false;
//                    }
//                    return true;
//                }, r -> {
//                    if (r != null && r.isFile()) {
//                        String file = r.getName().toLowerCase();
//                        for (var project : LDLibRegistries.PROJECTS.values().stream()
//                                .map(AutoRegistry.Holder::value).map(Supplier::get)
//                                .filter(getProjectPredicate()).toList()) {
//                            if (file.endsWith("." + project.getSuffix())) {
//                                var p = project.loadProject(r.toPath());
//                                if (p != null) {
//                                    if (editor.isCurrentProjectSaved()) {
//                                        editor.loadProject(p);
//                                        editor.setCurrentProjectFile(r);
//                                    } else {
//                                        editor.askToSaveProject(result -> {
//                                            editor.loadProject(p);
//                                            editor.setCurrentProjectFile(r);
//                                        });
//                                    }
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                });
//    }

}
