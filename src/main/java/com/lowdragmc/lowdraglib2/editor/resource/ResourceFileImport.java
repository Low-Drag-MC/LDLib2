package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Turns a file the player dropped into something Minecraft can address, for the resource types whose
 * values are built from a {@link Identifier} — an image for a texture, a json for a model.
 * <p>
 * A file that already lives in the {@code ldlib2} asset pack is addressable as it is. Anything else,
 * a file from the desktop say, has to be brought into the pack first, so the player is asked where it
 * should go and it is copied there.
 */
public final class ResourceFileImport {
    /** The namespace files are imported under: the pack rooted at {@code <gamedir>/ldlib2}. */
    public static final String IMPORT_NAMESPACE = LDLib2.MOD_ID;

    private ResourceFileImport() {}

    /**
     * Reduces a file name to the characters a {@link Identifier} path accepts, so a file called
     * "My Image (2).png" can still become a resource. Path separators are replaced too, this is a
     * single name rather than a path.
     */
    public static String sanitizeName(String raw) {
        var builder = new StringBuilder(raw.length());
        for (var character : raw.toLowerCase(Locale.ROOT).toCharArray()) {
            builder.append(character != '/' && Identifier.isAllowedInIdentifier(character)
                    ? character : '_');
        }
        var sanitized = builder.toString();
        return sanitized.isBlank() ? "imported" : sanitized;
    }

    /**
     * The location a file is already reachable at, or null if it is not inside the ldlib2 asset pack.
     * Files elsewhere on disk are not addressable no matter what their path looks like, so an
     * {@code assets} folder on the desktop does not count.
     */
    @Nullable
    public static Identifier locationOf(File file) {
        try {
            var assets = LDLib2.getAssetsDir().getCanonicalFile().toPath();
            if (!file.getCanonicalFile().toPath().startsWith(assets)) return null;
        } catch (IOException e) {
            return null;
        }
        return new FilePath(file).getLocation();
    }

    /**
     * Makes the file addressable and hands the location back.
     * <p>
     * When it already is, that happens right away. Otherwise the player is asked where the file should
     * be copied to inside the pack, so {@code onResolved} may run much later, or never if they cancel.
     *
     * @param owner       the element to show the dialog on.
     * @param assetFolder the folder inside the namespace to suggest, e.g. {@code textures} or {@code models}.
     */
    public static void resolveOrImport(UIElement owner, File file, String assetFolder,
                                       Consumer<Identifier> onResolved, Runnable onCancel) {
        var existing = locationOf(file);
        if (existing != null) {
            onResolved.accept(existing);
            return;
        }
        var name = file.getName();
        var dot = name.indexOf('.');
        var extension = dot == -1 ? "" : name.substring(dot);
        var suggestion = assetFolder + "/" + sanitizeName(dot == -1 ? name : name.substring(0, dot)) + extension;

        Dialog.stringEditorDialog("editor.resource.import_destination", suggestion,
                Identifier::isValidPath,
                path -> {
                    if (path == null || !Identifier.isValidPath(path)) {
                        showError(owner, "editor.resource.import_invalid_path");
                        onCancel.run();
                        return;
                    }
                    var target = freeTarget(path);
                    var destination = new File(LDLib2.getAssetsDir(), IMPORT_NAMESPACE + "/" + target);
                    try {
                        var parent = destination.getParentFile();
                        if (parent != null && !parent.exists() && !parent.mkdirs()) {
                            showError(owner, "editor.resource.import_failed");
                            onCancel.run();
                            return;
                        }
                        Files.copy(file.toPath(), destination.toPath());
                    } catch (IOException e) {
                        LDLib2.LOGGER.error("Failed to import {} into the assets pack: ", file, e);
                        showError(owner, "editor.resource.import_failed");
                        onCancel.run();
                        return;
                    }
                    onResolved.accept(Identifier.fromNamespaceAndPath(IMPORT_NAMESPACE, target));
                }).show(owner);
    }

    /**
     * A variant of the requested path that is not taken yet, so importing the same file twice never
     * overwrites what the first import produced.
     */
    private static String freeTarget(String path) {
        var root = new File(LDLib2.getAssetsDir(), IMPORT_NAMESPACE);
        if (!new File(root, path).exists()) return path;
        var dot = path.lastIndexOf('.');
        var base = dot == -1 ? path : path.substring(0, dot);
        var extension = dot == -1 ? "" : path.substring(dot);
        var count = 1;
        while (new File(root, base + "_" + count + extension).exists()) {
            count++;
        }
        return base + "_" + count + extension;
    }

    private static void showError(UIElement owner, String messageKey) {
        Dialog.showNotification("editor.resource.import_failed", messageKey, null).show(owner);
    }
}
