package com.lowdragmc.lowdraglib2.editor.ui.browser;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Plain file system operations for the {@link AssetBrowser}, plus the prompts that guard them.
 * <p>
 * {@link com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog#showFileDialog} has an equivalent set of
 * actions, but they live in a package-private class with private members and are wired to the file
 * dialog itself, so they cannot be shared.
 */
public final class FileOps {
    /** Characters no file name may contain on the platforms we support. */
    public static final Pattern INVALID_NAME = Pattern.compile("[\\\\/:*?\"<>|]");

    private FileOps() {}

    public static boolean isValidName(@Nullable String name) {
        return name != null && !name.isBlank() && !name.equals(".") && !name.equals("..") &&
                !INVALID_NAME.matcher(name).find();
    }

    /** Whether a single character may be typed into a file name, for inline rename fields. */
    public static boolean isValidNameChar(char character) {
        return !INVALID_NAME.matcher(String.valueOf(character)).find();
    }

    /**
     * Deletes a file, or a directory with everything in it. Symbolic links are removed, not followed.
     */
    public static boolean deleteRecursively(File file) {
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

    /**
     * A non-existing file in {@code directory} based on {@code name}, appending " (1)", " (2)", ...
     * before the extension until the name is free.
     */
    public static File uniqueTarget(File directory, String name) {
        var target = new File(directory, name);
        if (!target.exists()) return target;
        // split on the FIRST dot so compound resource extensions (".texture.nbt") stay intact
        var dot = name.indexOf('.');
        var base = dot == -1 ? name : name.substring(0, dot);
        var extension = dot == -1 ? "" : name.substring(dot);
        var count = 1;
        while (target.exists()) {
            target = new File(directory, base + " (" + count + ")" + extension);
            count++;
        }
        return target;
    }

    /**
     * Copies a file, or a directory with everything in it, into {@code destDir}.
     *
     * @return where the copy actually landed — the name is uniquified when it is already taken — or
     *         null if the copy was rejected or failed.
     */
    @Nullable
    public static File copyInto(File src, File destDir) {
        if (!src.exists() || !destDir.isDirectory()) return null;
        if (isDescendant(destDir, src)) return null;
        var target = uniqueTarget(destDir, src.getName());
        return copyTo(src, target) ? target : null;
    }

    private static boolean copyTo(File src, File target) {
        try {
            if (src.isDirectory()) {
                if (!target.mkdirs()) return false;
                var children = src.listFiles();
                if (children != null) {
                    for (var child : children) {
                        if (!copyTo(child, new File(target, child.getName()))) return false;
                    }
                }
                return true;
            }
            var parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
            Files.copy(src.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            return true;
        } catch (IOException e) {
            LDLib2.LOGGER.error("Failed to copy {} to {}: ", src, target, e);
            return false;
        }
    }

    /**
     * Moves a file or directory into {@code destDir}. Rejects moving a directory into itself or into
     * one of its own descendants, which would otherwise recurse forever.
     *
     * @return where the file actually ended up — the name is uniquified when it is already taken — or
     *         null if the move was rejected or failed.
     */
    @Nullable
    public static File moveInto(File src, File destDir) {
        if (!src.exists() || !destDir.isDirectory()) return null;
        if (src.equals(destDir) || isDescendant(destDir, src)) return null;
        if (destDir.equals(src.getParentFile())) return null;
        var target = uniqueTarget(destDir, src.getName());
        if (src.renameTo(target)) return target;
        // across file system boundaries renameTo fails, fall back to copy + delete
        if (copyTo(src, target)) {
            if (!deleteRecursively(src)) {
                LDLib2.LOGGER.error("Moved {} to {} but failed to remove the original", src, target);
            }
            return target;
        }
        return null;
    }

    /** Whether {@code candidate} is {@code ancestor} itself or lives inside it. */
    public static boolean isDescendant(File candidate, File ancestor) {
        try {
            var candidatePath = candidate.getCanonicalFile().toPath();
            var ancestorPath = ancestor.getCanonicalFile().toPath();
            return candidatePath.startsWith(ancestorPath);
        } catch (IOException e) {
            return candidate.getAbsolutePath().startsWith(ancestor.getAbsolutePath());
        }
    }

    public static void promptNewFolder(UIElement owner, File parentDir, Runnable onDone) {
        promptName(owner, "ldlib.gui.file_dialog.new_folder", "", name -> {
            var folder = new File(parentDir, name);
            if (folder.exists()) {
                showError(owner, "ldlib.gui.file_dialog.name_exists");
            } else if (!folder.mkdirs()) {
                showError(owner, "ldlib.gui.file_dialog.failed");
            } else {
                onDone.run();
            }
        });
    }

    public static void promptRename(UIElement owner, File file, Runnable onDone) {
        promptName(owner, "ldlib.gui.file_dialog.rename", file.getName(), name -> {
            var renamed = new File(file.getParentFile(), name);
            if (renamed.equals(file)) return;
            if (renamed.exists()) {
                showError(owner, "ldlib.gui.file_dialog.name_exists");
            } else if (!file.renameTo(renamed)) {
                showError(owner, "ldlib.gui.file_dialog.failed");
            } else {
                onDone.run();
            }
        });
    }

    public static void confirmDelete(UIElement owner, File file, Runnable onDone) {
        Dialog.showCheckBox("ldlib.gui.file_dialog.delete",
                Component.translatable("ldlib.gui.file_dialog.delete.confirm", file.getName()),
                confirmed -> {
                    if (!confirmed) return;
                    if (deleteRecursively(file)) {
                        onDone.run();
                    } else {
                        showError(owner, "ldlib.gui.file_dialog.failed");
                    }
                }).show(owner);
    }

    private static void promptName(UIElement owner, String title, String initial, Consumer<String> onName) {
        Dialog.stringEditorDialog(title, initial, FileOps::isValidName, name -> {
            var trimmed = name == null ? "" : name.trim();
            if (!isValidName(trimmed)) {
                showError(owner, "ldlib.gui.file_dialog.invalid_name");
                return;
            }
            onName.accept(trimmed);
        }).show(owner);
    }

    /** Reports a failed inline rename, which has no dialog of its own to report into. */
    public static void showRenameError(UIElement owner, boolean nameExists) {
        showError(owner, nameExists ? "ldlib.gui.file_dialog.name_exists" : "ldlib.gui.file_dialog.failed");
    }

    private static void showError(UIElement owner, String info) {
        Dialog.showNotification("ldlib.gui.file_dialog.error", info, null).show(owner);
    }
}
