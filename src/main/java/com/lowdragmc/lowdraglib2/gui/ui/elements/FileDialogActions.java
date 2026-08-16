package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog.FileFeature;
import com.lowdragmc.lowdraglib2.gui.util.FileNode;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

import java.io.File;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * The file management actions of {@link Dialog#showFileDialog}'s tree: new folder, rename and delete,
 * offered as a right click menu on the tree.
 *
 * <p>Nothing here refreshes the tree: the {@link TreeList} diffs its displayed children each tick
 * against {@link FileNode#getChildren()}, which follows the directory on its own, so the view keeps up
 * with whatever these actions (or anything else on the machine) do to it.</p>
 */
final class FileDialogActions {
    private FileDialogActions() {}

    /** Characters that are not portable in a file name. */
    private static final Pattern INVALID_NAME = Pattern.compile("[\\\\/:*?\"<>|]");

    /**
     * Opens the file tree's context menu at the given world position. The actions apply to the hovered
     * node, or to the tree's root when the click didn't land on a row, and are filtered by the dialog's
     * {@link FileFeature} mask — nothing opens when none of them is enabled.
     */
    static void openContextMenu(Dialog dialog, TreeList<FileNode> treeList, FileNode root, int features, float x, float y) {
        var mui = dialog.getModularUI();
        if (mui == null || dialog.getParent() == null) return;
        var hovered = treeList.getHoveredNode();
        var target = hovered == null ? root : hovered;
        var file = target.getKey();

        var menuBuilder = TreeBuilder.Menu.start();
        // a new folder goes into the hovered directory, or next to the hovered file
        var parentDir = file.isDirectory() ? file : file.getParentFile();
        if (FileFeature.has(features, FileFeature.NEW_FOLDER) && parentDir != null && parentDir.isDirectory()) {
            menuBuilder.leaf(Icons.FOLDER, "ldlib.gui.file_dialog.new_folder", () -> newFolder(dialog, parentDir));
        }
        // the root is the dialog's own directory, renaming or deleting it would pull the rug out
        if (!root.equals(target)) {
            if (FileFeature.has(features, FileFeature.RENAME)) {
                menuBuilder.leaf(Icons.EDIT_FILE, "ldlib.gui.file_dialog.rename", () -> rename(dialog, file));
            }
            if (FileFeature.has(features, FileFeature.DELETE)) {
                menuBuilder.leaf(Icons.REMOVE_FILE, "ldlib.gui.file_dialog.delete", () -> delete(dialog, file));
            }
        }
        if (menuBuilder.isEmpty()) return;

        var rootElement = mui.ui.rootElement;
        var offset = rootElement.worldToLocalLayoutOffset(new Vector2f(x, y));
        var menu = new Menu<>(menuBuilder.build(), TreeBuilder.Menu::uiProvider)
                .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                .setOnNodeClicked(TreeBuilder.Menu::handle);
        menu.layout(layout -> layout.left(offset.x).top(offset.y));
        // the menu is anchored to the root element, so tell the dialog it's still "inside" it
        dialog.addExternalElement(menu);
        menu.setOnClose(() -> dialog.removeExternalElement(menu));
        rootElement.addChild(menu);
    }

    /** The directory the "open folder" button reveals: the selected one, falling back to the root. */
    static File openTargetDir(TreeList<FileNode> treeList, File root) {
        var selected = treeList.getSelected().stream().findFirst().map(FileNode::getKey).orElse(null);
        var dir = selected == null ? null : selected.isDirectory() ? selected : selected.getParentFile();
        if (dir != null && dir.isDirectory()) {
            return dir;
        }
        return root.isDirectory() ? root : root.getParentFile();
    }

    private static void newFolder(Dialog dialog, File parentDir) {
        promptName(dialog, "ldlib.gui.file_dialog.new_folder", "", name -> {
            var folder = new File(parentDir, name);
            if (folder.exists()) {
                showError(dialog, "ldlib.gui.file_dialog.name_exists");
            } else if (!folder.mkdirs()) {
                showError(dialog, "ldlib.gui.file_dialog.failed");
            }
        });
    }

    private static void rename(Dialog dialog, File file) {
        promptName(dialog, "ldlib.gui.file_dialog.rename", file.getName(), name -> {
            var renamed = new File(file.getParentFile(), name);
            if (renamed.equals(file)) return;
            if (renamed.exists()) {
                showError(dialog, "ldlib.gui.file_dialog.name_exists");
            } else if (!file.renameTo(renamed)) {
                showError(dialog, "ldlib.gui.file_dialog.failed");
            }
        });
    }

    private static void delete(Dialog dialog, File file) {
        showNested(dialog, Dialog.showCheckBox("ldlib.gui.file_dialog.delete",
                Component.translatable("ldlib.gui.file_dialog.delete.confirm", file.getName()),
                confirmed -> {
                    if (confirmed && !deleteRecursively(file)) {
                        showError(dialog, "ldlib.gui.file_dialog.failed");
                    }
                }));
    }

    /** Asks for a file name, rejecting the ones the file system won't take. */
    private static void promptName(Dialog dialog, String title, String initial, Consumer<String> onName) {
        showNested(dialog, Dialog.stringEditorDialog(title, initial, null, name -> {
            var trimmed = name.trim();
            if (isValidName(trimmed)) {
                onName.accept(trimmed);
            } else {
                showError(dialog, "ldlib.gui.file_dialog.invalid_name");
            }
        }));
    }

    private static void showError(Dialog dialog, String info) {
        showNested(dialog, Dialog.showNotification("editor.error", info, null));
    }

    /** Shows a follow-up dialog on top of the file dialog, without letting the latter auto-close. */
    private static void showNested(Dialog dialog, Dialog nested) {
        var parent = dialog.getParent();
        if (parent == null) return;
        dialog.addExternalElement(nested);
        nested.setOnClose(() -> dialog.removeExternalElement(nested));
        nested.show(parent);
    }

    private static boolean isValidName(String name) {
        return !name.isBlank() && !name.equals(".") && !name.equals("..") && !INVALID_NAME.matcher(name).find();
    }

    /** Deletes a file, or a directory with everything in it. Symbolic links are removed, not followed. */
    private static boolean deleteRecursively(File file) {
        if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
            var children = file.listFiles();
            if (children != null) {
                for (var child : children) {
                    if (!deleteRecursively(child)) return false;
                }
            }
        }
        return file.delete();
    }
}
