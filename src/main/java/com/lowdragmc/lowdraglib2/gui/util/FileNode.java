package com.lowdragmc.lowdraglib2.gui.util;

import com.lowdragmc.lowdraglib2.utils.FileUtility;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A directory tree node backed by the file system.
 * <p>
 * {@link #getChildren()} is polled — the trees built on top of it diff their children every tick so they
 * follow the file system without a refresh button — so the listing is cached and revalidated against the
 * directory's modification time, which every mainstream file system moves when an entry is added, removed
 * or renamed (and leaves alone when a child's <em>contents</em> change, which is exactly the distinction
 * the child set cares about).
 * <p>
 * That stamp is not enough on its own: a file time comes from a clock that only advances every ~15ms on
 * Windows, so a change made right after a listing can leave the directory looking untouched. The cache
 * therefore also ages out, which bounds how long any change can stay hidden. The window is short enough
 * to read as immediate and long enough that the re-read costs a fraction of what polling raw
 * {@link File#listFiles()} did.
 */
public class FileNode implements ITreeNode<File, Void> {
    /** How long a cached listing is trusted before the directory is re-read regardless of its stamp. */
    private static final long MAX_CACHE_AGE_NS = 250_000_000L;
    /** Directories first, then everything else, each ordered the way the platform orders paths. */
    private static final Comparator<FileNode> CHILD_ORDER =
            Comparator.comparing((FileNode node) -> !node.isDirectory()).thenComparing(FileNode::getKey);

    @Nullable
    @Getter
    public final FileNode parent;
    @Getter
    public final int dimension;
    @Getter
    public final File key;
    @Nullable
    protected Predicate<FileNode> valid;

    // Attributes, taken from the parent's listing when this node came out of one. Resolved on demand
    // otherwise (a node built directly, i.e. a root), and then kept: a node only outlives a change to
    // its own entry for as long as its parent's cached listing does.
    @Nullable
    private Boolean directory;
    @Nullable
    private Boolean regularFile;

    // Cached listing, valid while the directory's stamp holds and the entry has not aged out.
    @Nullable
    private List<FileNode> childrenCache;
    private long cacheStamp;
    private long cacheTime;

    public FileNode(File dir){
        this(null, 0, dir);
    }

    private FileNode(@Nullable FileNode parent, int dimension, File key) {
        this.parent = parent;
        this.dimension = dimension;
        this.key = key;
    }

    /**
     * Sets the filter every node in this subtree is passed through before it is listed as a child.
     */
    public FileNode setValid(@Nullable Predicate<FileNode> valid) {
        this.valid = valid;
        this.childrenCache = null; // a different filter means a different child set
        return this;
    }

    /**
     * Drops the cached listing, so the next {@link #getChildren()} re-reads the directory. Only needed
     * when a change has to be picked up before the poll would notice it — the poll notices on its own.
     */
    public void invalidate() {
        childrenCache = null;
    }

    @Override
    public @Nullable Void getContent() {
        return null;
    }

    /**
     * Whether this entry is a directory, answered from the parent's listing rather than a fresh stat.
     */
    public boolean isDirectory() {
        if (directory == null) {
            directory = key.isDirectory();
        }
        return directory;
    }

    /**
     * Whether this entry is a file, answered from the parent's listing rather than a fresh stat.
     * <p>
     * Prefer this to {@code getKey().isFile()} — the {@link File} handed out by {@link #getKey()} knows
     * nothing, so every question asked of it is another syscall, and a filter that asks one per entry is
     * what made large folders expensive in the first place.
     */
    public boolean isFile() {
        if (regularFile == null) {
            regularFile = key.isFile();
        }
        return regularFile;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Leafness here is structural rather than by child count: a file is a leaf and a directory is not,
     * so an empty folder still shows as something to expand.
     */
    @Override
    public boolean isLeaf() {
        return isFile();
    }

    @Override
    @Nonnull
    public List<FileNode> getChildren() {
        var cached = childrenCache;
        if (cached != null && !isCacheStale()) {
            return cached;
        }
        // read before listing: a change that lands mid-listing then leaves a stamp we do not match,
        // so the next poll re-reads rather than trusting a half-old snapshot
        var stamp = key.lastModified();
        var children = new ArrayList<FileNode>();
        for (var entry : FileUtility.listDirectory(key)) {
            var node = new FileNode(this, dimension + 1, entry.file());
            node.directory = entry.directory();
            node.regularFile = entry.regularFile();
            node.valid = valid;
            if (valid != null && !valid.test(node)) continue;
            children.add(node);
        }
        children.sort(CHILD_ORDER);
        // unmodifiable because it is handed out repeatedly rather than rebuilt per call, so a caller
        // that edited it would be corrupting every later reader
        childrenCache = Collections.unmodifiableList(children);
        cacheStamp = stamp;
        cacheTime = System.nanoTime();
        return childrenCache;
    }

    private boolean isCacheStale() {
        // nanoTime, not the wall clock: it cannot step backwards, so there is no "the clock moved" case
        return System.nanoTime() - cacheTime >= MAX_CACHE_AGE_NS || key.lastModified() != cacheStamp;
    }

    @Override
    public String toString() {
        return getKey().getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileNode fileNode = (FileNode) o;
        return dimension == fileNode.dimension &&
                Objects.equals(key, fileNode.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, key);
    }
}
