package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Remembers, across sessions, which projects were opened recently and where the asset browser was left
 * in each of them.
 * <p>
 * Paths are stored in the game-relative form {@link FilePath} uses, so moving the instance somewhere
 * else, or sharing it, does not invalidate the entries.
 */
public final class EditorProjectStore {
    private static final String RECENT_KEY = "recent";
    private static final String BROWSER_PATHS_KEY = "browserPaths";

    private EditorProjectStore() {}

    private static File getFile() {
        var dir = new File(LDLib2.getAssetsDir().getParentFile(), "editor_layouts");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "projects.nbt");
    }

    private static CompoundTag read() {
        var file = getFile();
        if (!file.exists()) return new CompoundTag();
        try {
            var tag = NbtIo.read(file.toPath());
            return tag == null ? new CompoundTag() : tag;
        } catch (Exception e) {
            return new CompoundTag();
        }
    }

    private static void write(CompoundTag tag) {
        try {
            NbtIo.write(tag, getFile().toPath());
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to save the editor project store: ", e);
        }
    }

    /**
     * The projects opened most recently, newest first. Entries whose file has since disappeared are
     * left out, so the caller never offers a project that cannot be opened.
     */
    public static List<File> getRecentProjects() {
        var recent = read().getList(RECENT_KEY, Tag.TAG_STRING);
        var files = new ArrayList<File>(recent.size());
        for (var entry : recent) {
            var file = FilePath.resolveFile(entry.getAsString());
            if (file.isFile()) {
                files.add(file);
            }
        }
        return files;
    }

    /**
     * Records a project as the most recently opened one.
     *
     * @param limit how many to keep. Zero or less disables the list and clears it.
     */
    public static void addRecentProject(File projectFile, int limit) {
        var path = FilePath.toGameRelative(projectFile.getPath());
        var tag = read();
        var recent = new ListTag();
        if (limit > 0) {
            recent.add(StringTag.valueOf(path));
            for (var entry : tag.getList(RECENT_KEY, Tag.TAG_STRING)) {
                if (recent.size() >= limit) break;
                // the project just opened moved to the front, drop the older mention of it
                if (entry.getAsString().equals(path)) continue;
                // a project that has since been deleted would otherwise keep a slot forever and push
                // out projects that do still exist
                if (!FilePath.resolveFile(entry.getAsString()).isFile()) continue;
                recent.add(entry);
            }
        }
        tag.put(RECENT_KEY, recent);
        write(tag);
    }

    public static void clearRecentProjects() {
        var tag = read();
        tag.put(RECENT_KEY, new ListTag());
        write(tag);
    }

    /** The directory the asset browser was showing the last time this project was open. */
    @Nullable
    public static File getBrowserPath(File projectFile) {
        var paths = read().getCompound(BROWSER_PATHS_KEY);
        var key = FilePath.toGameRelative(projectFile.getPath());
        if (!paths.contains(key)) return null;
        var directory = FilePath.resolveFile(paths.getString(key));
        return directory.isDirectory() ? directory : null;
    }

    public static void setBrowserPath(File projectFile, File directory) {
        var tag = read();
        var paths = tag.getCompound(BROWSER_PATHS_KEY);
        paths.putString(FilePath.toGameRelative(projectFile.getPath()),
                FilePath.toGameRelative(directory.getPath()));
        tag.put(BROWSER_PATHS_KEY, paths);
        write(tag);
    }
}
