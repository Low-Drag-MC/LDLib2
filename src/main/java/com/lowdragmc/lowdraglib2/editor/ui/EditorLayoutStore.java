package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.floating.FloatingLayout;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persists {@link EditorLayout} snapshots per {@code ProjectType.name} into the user's game directory.
 * One file per project type; later writes overwrite earlier ones (last-closed wins).
 */
public final class EditorLayoutStore {
    private static final String FLOATING_KEY = "floating";

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
        save(projectTypeName, layout, List.of());
    }

    /**
     * Writes the docked layout and any floating windows to the same file.
     *
     * <p>The floating windows go under their own key, so a file written by an older version — which
     * has no such key — still loads, and one written here still loads in an older version.
     */
    public static void save(String projectTypeName, EditorLayout layout, List<FloatingLayout> floating) {
        try {
            var tag = layout.serialize();
            if (!floating.isEmpty()) {
                var list = new ListTag();
                for (var window : floating) {
                    list.add(window.serialize());
                }
                tag.put(FLOATING_KEY, list);
            }
            NbtIo.write(tag, getFile(projectTypeName).toPath());
        } catch (Exception ignored) {}
    }

    public static Optional<EditorLayout> load(String projectTypeName) {
        return read(projectTypeName).map(EditorLayout::deserialize);
    }

    /**
     * The saved floating windows, or an empty list when the file has none — including every file
     * written before floating windows existed.
     */
    public static List<FloatingLayout> loadFloating(String projectTypeName) {
        var tag = read(projectTypeName).orElse(null);
        if (tag == null || !tag.contains(FLOATING_KEY)) return List.of();
        var list = tag.getListOrEmpty(FLOATING_KEY);
        var floating = new ArrayList<FloatingLayout>(list.size());
        for (int i = 0; i < list.size(); i++) {
            try {
                floating.add(FloatingLayout.deserialize(list.getCompoundOrEmpty(i)));
            } catch (Exception ignored) {
                // One unreadable window must not cost the user the rest of the layout.
            }
        }
        return floating;
    }

    private static Optional<CompoundTag> read(String projectTypeName) {
        var file = getFile(projectTypeName);
        if (!file.exists()) return Optional.empty();
        try {
            return Optional.ofNullable(NbtIo.read(file.toPath()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
