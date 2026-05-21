package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;

/**
 * Persists {@link EditorLayout} snapshots per {@code ProjectType.name} into the user's game directory.
 * One file per project type; later writes overwrite earlier ones (last-closed wins).
 */
public final class EditorLayoutStore {
    private EditorLayoutStore() {}

    private static File getStoreDir() {
        var dir = new File(LDLib2.getAssetsDir().getParentFile(), "editor_layouts");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static File getFile(String projectTypeName) {
        return new File(getStoreDir(), sanitize(projectTypeName) + ".nbt");
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    public static void save(String projectTypeName, EditorLayout layout) {
        try {
            NbtIo.write(layout.serialize(), getFile(projectTypeName).toPath());
        } catch (Exception ignored) {}
    }

    public static Optional<EditorLayout> load(String projectTypeName) {
        var file = getFile(projectTypeName);
        if (!file.exists()) return Optional.empty();
        try {
            var tag = NbtIo.read(file.toPath());
            if (tag == null) return Optional.empty();
            return Optional.of(EditorLayout.deserialize(tag));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
