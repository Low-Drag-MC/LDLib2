package com.lowdragmc.lowdraglib2.editor.ui.browser;

import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A name search over everything under a folder, walked on a virtual thread. Matches are queued as they
 * are found for the browser to take off per tick; a new query is a new instance and the old one is
 * {@link #cancel() cancelled}.
 * <p>
 * Only the name is matched, the way the grid shows it (a resource file without its extension); the
 * browser's other filters are applied as the matches are taken off. Free of game classes so it can be
 * unit tested.
 */
public final class AssetSearch {
    /** Carries the attributes the walk already read, so the browser never stats the file again. */
    public record Hit(File file, boolean directory, long size, long lastModified) {}

    @Nullable
    private final Thread thread;
    private final ConcurrentLinkedQueue<Hit> hits = new ConcurrentLinkedQueue<>();
    private final File root;
    /** Lower-cased. */
    private final String query;
    private final List<String> resourceExtensions;
    private final int limit;

    private volatile boolean cancelled;
    private volatile boolean finished;
    private volatile boolean truncated;
    private volatile int found;

    private AssetSearch(File root, String query, List<String> resourceExtensions, int limit, boolean async) {
        this.root = root;
        this.query = query.toLowerCase(Locale.ROOT);
        this.resourceExtensions = List.copyOf(resourceExtensions);
        this.limit = limit;
        this.thread = async ? Thread.ofVirtual().name("ldlib2-asset-search").start(this::run) : null;
    }

    /**
     * @param resourceExtensions snapshotted by the caller, the resource view is not thread safe.
     * @param limit              the walk stops at this many matches.
     */
    public static AssetSearch start(File root, String query, List<String> resourceExtensions, int limit) {
        return new AssetSearch(root, query, resourceExtensions, limit, true);
    }

    /** Runs on the calling thread and returns finished, for tests. */
    public static AssetSearch runNow(File root, String query, List<String> resourceExtensions, int limit) {
        var search = new AssetSearch(root, query, resourceExtensions, limit, false);
        search.run();
        return search;
    }

    public String getQuery() {
        return query;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /** Done walking: the whole tree seen, the limit hit, or cancelled. */
    public boolean isFinished() {
        return finished;
    }

    /** Stopped at the limit, so there may be more matches. */
    public boolean isTruncated() {
        return truncated;
    }

    public int getFound() {
        return found;
    }

    @Nullable
    public Hit poll() {
        return hits.poll();
    }

    private void run() {
        try {
            Files.walkFileTree(root.toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<>() {
                        @Override
                        @Nonnull
                        public FileVisitResult preVisitDirectory(@Nonnull Path dir, @Nonnull BasicFileAttributes attrs) {
                            if (cancelled) return FileVisitResult.TERMINATE;
                            if (!dir.equals(root.toPath()) && !offer(dir, attrs, true)) {
                                return FileVisitResult.TERMINATE;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        @Nonnull
                        public FileVisitResult visitFile(@Nonnull Path file, @Nonnull BasicFileAttributes attrs) {
                            if (cancelled) return FileVisitResult.TERMINATE;
                            // a directory lands here only when it cannot be descended into
                            return offer(file, attrs, attrs.isDirectory())
                                    ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
                        }

                        @Override
                        @Nonnull
                        public FileVisitResult visitFileFailed(@Nonnull Path file, @Nonnull IOException e) {
                            return cancelled ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException | RuntimeException ignored) {
        } finally {
            finished = true;
        }
    }

    /** @return false once the limit is reached. */
    private boolean offer(Path path, BasicFileAttributes attrs, boolean directory) {
        var name = path.getFileName();
        if (name == null || !matches(name.toString(), directory, query, resourceExtensions)) return true;
        hits.add(new Hit(path.toFile(), directory, attrs.size(), attrs.lastModifiedTime().toMillis()));
        found++;
        if (found >= limit) {
            truncated = true;
            return false;
        }
        return true;
    }

    /** @param lowerQuery already lower-cased; an empty one matches everything. */
    public static boolean matches(String fileName, boolean directory, String lowerQuery, List<String> resourceExtensions) {
        if (lowerQuery.isEmpty()) return true;
        var shown = directory ? fileName : displayName(fileName, resourceExtensions);
        return shown.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    /**
     * The name without the longest resource extension it ends with, the rule
     * {@link ResourceBehaviorCache#resourceOf} resolves a file's type by.
     */
    public static String displayName(String fileName, List<String> resourceExtensions) {
        var best = 0;
        for (var extension : resourceExtensions) {
            if (fileName.length() > extension.length() && fileName.endsWith(extension) && extension.length() > best) {
                best = extension.length();
            }
        }
        return fileName.substring(0, fileName.length() - best);
    }
}
