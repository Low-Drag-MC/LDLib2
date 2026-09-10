package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Remembers, across sessions, which projects were opened recently and where the asset browser was left
 * in each of them.
 * <p>
 * Paths are stored in the game-relative form {@link FilePath} uses, so moving the instance somewhere
 * else, or sharing it, does not invalidate the entries.
 * <p>
 * Recent projects are kept per {@link ProjectType}, the same way {@link EditorLayoutStore} keeps
 * layouts: every editor lists, evicts and clears only the projects it can actually open, and never the
 * ones belonging to another editor that happens to share this file.
 */
public final class EditorProjectStore {
    private static final String RECENT_KEY = "recent";
    private static final String PATH_KEY = "path";
    private static final String TYPE_KEY = "type";
    private static final String BROWSER_PATHS_KEY = "browserPaths";

    /**
     * One remembered project: where it lives, and the {@link ProjectType#getName() project type} it was
     * opened as. The type is empty for an entry written before recents were kept per type; such an entry
     * is claimed by whichever type recognises its file name.
     * <p>
     * Package-private, along with the methods that pick entries apart, so the per-type bookkeeping can be
     * tested without a game directory to read and write.
     */
    record RecentEntry(String path, String type) {}

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
     * The remembered projects, newest first, exactly as the file holds them — every project type
     * included.
     * <p>
     * Both shapes the file may have are read here: entries written since recents became per-type, and
     * the bare paths written before that, which come back with an empty type.
     */
    static List<RecentEntry> readEntries(CompoundTag tag) {
        var entries = new ArrayList<RecentEntry>();
        var recent = tag.getList(RECENT_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < recent.size(); i++) {
            var entry = recent.getCompound(i);
            var path = entry.getString(PATH_KEY);
            if (!path.isEmpty()) {
                entries.add(new RecentEntry(path, entry.getString(TYPE_KEY)));
            }
        }
        // a list holds one element type, so at most one of these two loops ever finds anything
        for (var legacy : tag.getList(RECENT_KEY, Tag.TAG_STRING)) {
            entries.add(new RecentEntry(legacy.getAsString(), ""));
        }
        return entries;
    }

    static void writeEntries(CompoundTag tag, List<RecentEntry> entries) {
        var recent = new ListTag();
        for (var entry : entries) {
            var compound = new CompoundTag();
            compound.putString(PATH_KEY, entry.path());
            if (!entry.type().isEmpty()) {
                compound.putString(TYPE_KEY, entry.type());
            }
            recent.add(compound);
        }
        tag.put(RECENT_KEY, recent);
    }

    /**
     * Whether the entry is one of the given type's projects. An entry from before recents were kept per
     * type carries no type name and is matched on its file name instead, so a list that is already on
     * disk keeps working and ends up split between the editors that can open it.
     */
    static boolean matches(RecentEntry entry, ProjectType type) {
        return entry.type().isEmpty()
                ? entry.path().endsWith(type.getSuffix())
                : entry.type().equals(type.getName());
    }

    static boolean matches(RecentEntry entry, Collection<ProjectType> types) {
        return types.stream().anyMatch(type -> matches(entry, type));
    }

    /**
     * The projects opened most recently as one of the given types, newest first. Entries whose file has
     * since disappeared are left out, so the caller never offers a project that cannot be opened.
     *
     * @param types the project types the asking editor can open. Projects of any other type are left to
     *              the editor that owns them.
     */
    public static List<File> getRecentProjects(Collection<ProjectType> types) {
        var files = new ArrayList<File>();
        for (var entry : selectRecentProjects(readEntries(read()), types)) {
            var file = FilePath.resolveFile(entry.path());
            if (file.isFile()) {
                files.add(file);
            }
        }
        return files;
    }

    static List<RecentEntry> selectRecentProjects(List<RecentEntry> entries, Collection<ProjectType> types) {
        if (types.isEmpty()) return List.of();
        return entries.stream().filter(entry -> matches(entry, types)).toList();
    }

    /**
     * Records a project as the most recently opened one of its type.
     *
     * @param limit how many of this type to keep. Zero or less disables the list and clears it, for this
     *              type alone.
     */
    public static void addRecentProject(File projectFile, ProjectType type, int limit) {
        var tag = read();
        // a project of this type that has since been deleted would otherwise keep a slot forever and
        // push out projects that do still exist. Another type's entries are its own editor's to prune.
        var entries = readEntries(tag).stream()
                .filter(entry -> !matches(entry, type) || FilePath.resolveFile(entry.path()).isFile())
                .toList();
        var path = FilePath.toGameRelative(projectFile.getPath());
        writeEntries(tag, addRecentProject(entries, path, type, limit));
        write(tag);
    }

    static List<RecentEntry> addRecentProject(List<RecentEntry> entries, String path, ProjectType type,
                                              int limit) {
        var kept = new ArrayList<RecentEntry>();
        var mine = 0;
        if (limit > 0) {
            kept.add(new RecentEntry(path, type.getName()));
            mine = 1;
        }
        for (var entry : entries) {
            // another type's projects belong to another editor: they are neither counted against this
            // limit nor dropped by it
            if (!matches(entry, type)) {
                kept.add(entry);
                continue;
            }
            if (mine >= limit) continue;
            // the project just opened moved to the front, drop the older mention of it
            if (entry.path().equals(path)) continue;
            kept.add(entry);
            mine++;
        }
        return kept;
    }

    /**
     * Forgets the recent projects of the given types, leaving every other type's list alone.
     */
    public static void clearRecentProjects(Collection<ProjectType> types) {
        var tag = read();
        writeEntries(tag, clearRecentProjects(readEntries(tag), types));
        write(tag);
    }

    static List<RecentEntry> clearRecentProjects(List<RecentEntry> entries, Collection<ProjectType> types) {
        return entries.stream().filter(entry -> !matches(entry, types)).toList();
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
