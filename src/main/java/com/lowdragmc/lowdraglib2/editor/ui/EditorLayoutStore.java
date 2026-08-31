package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.floating.FloatingLayout;
import com.lowdragmc.lowdraglib2.gui.ui.window.WindowBounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persists {@link EditorLayout} snapshots per {@code ProjectType.name} into the user's game directory.
 * One file per project type; later writes overwrite earlier ones (last-closed wins).
 */
public final class EditorLayoutStore {
    private static final String FLOATING_KEY = "floating";
    private static final String FLOATING_BOUNDS_KEY = "floatingBounds";

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
        save(projectTypeName, layout, List.of(), Map.of());
    }

    /**
     * Writes the docked layout, any floating windows, and where each floated view was last left.
     *
     * <p>Each goes under its own key, so a file written by an older version — which has neither —
     * still loads, and one written here still loads in an older version.
     *
     * <p>The remembered rectangles are not the same thing as the floating windows: those describe
     * the windows that are open <em>now</em> and are re-opened next session, while these describe
     * where a view's window belongs whenever it is next torn out, including for views that are
     * docked at the moment.
     */
    public static void save(String projectTypeName, EditorLayout layout, List<FloatingLayout> floating,
                            Map<String, WindowBounds> floatingBounds) {
        try {
            var tag = layout.serialize();
            if (!floating.isEmpty()) {
                var list = new ListTag();
                for (var window : floating) {
                    list.add(window.serialize());
                }
                tag.put(FLOATING_KEY, list);
            }
            if (!floatingBounds.isEmpty()) {
                var list = new ListTag();
                floatingBounds.forEach((name, bounds) -> {
                    var entry = new CompoundTag();
                    entry.putString("name", name);
                    entry.putInt("x", bounds.x());
                    entry.putInt("y", bounds.y());
                    entry.putInt("width", bounds.width());
                    entry.putInt("height", bounds.height());
                    list.add(entry);
                });
                tag.put(FLOATING_BOUNDS_KEY, list);
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

    /**
     * Where each floated view's window was last left, keyed by view name. Empty for a file written
     * before this was recorded, in which case a first float sizes itself from the pane instead.
     */
    public static Map<String, WindowBounds> loadFloatingBounds(String projectTypeName) {
        var tag = read(projectTypeName).orElse(null);
        if (tag == null || !tag.contains(FLOATING_BOUNDS_KEY)) return Map.of();
        var list = tag.getListOrEmpty(FLOATING_BOUNDS_KEY);
        var bounds = new LinkedHashMap<String, WindowBounds>(list.size());
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompoundOrEmpty(i);
            var name = entry.getStringOr("name", "");
            if (name.isEmpty()) continue;
            bounds.put(name, new WindowBounds(entry.getIntOr("x", 0), entry.getIntOr("y", 0),
                    entry.getIntOr("width", 0), entry.getIntOr("height", 0)));
        }
        return bounds;
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
