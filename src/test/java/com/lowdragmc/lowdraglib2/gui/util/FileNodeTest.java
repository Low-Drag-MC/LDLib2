package com.lowdragmc.lowdraglib2.gui.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileNodeTest {
    @TempDir
    Path tempDir;

    private List<String> namesOf(FileNode node) {
        return node.getChildren().stream().map(child -> child.getKey().getName()).toList();
    }

    /**
     * Moves the directory's stamp, so a change is visible to the poll straight away rather than only once
     * the cache ages out. The file system does this itself, but off a clock coarse enough (~15ms on
     * Windows) that a test acting immediately cannot rely on it.
     */
    private void touch(Path directory) throws IOException {
        var moved = Files.getLastModifiedTime(directory).toMillis() + 2000;
        Files.setLastModifiedTime(directory, FileTime.fromMillis(moved));
    }

    @Test
    void listsDirectoriesBeforeFilesEachInNameOrder() throws IOException {
        Files.createDirectories(tempDir.resolve("zeta"));
        Files.createDirectories(tempDir.resolve("alpha"));
        Files.writeString(tempDir.resolve("b.txt"), "");
        Files.writeString(tempDir.resolve("a.txt"), "");

        assertEquals(List.of("alpha", "zeta", "a.txt", "b.txt"), namesOf(new FileNode(tempDir.toFile())));
    }

    @Test
    void resolvesTypeOfEachListedEntry() throws IOException {
        Files.createDirectories(tempDir.resolve("folder"));
        Files.writeString(tempDir.resolve("file.txt"), "");

        var children = new FileNode(tempDir.toFile()).getChildren();

        var folder = children.get(0);
        assertTrue(folder.isDirectory());
        assertFalse(folder.isLeaf());
        var file = children.get(1);
        assertFalse(file.isDirectory());
        assertTrue(file.isLeaf());
    }

    @Test
    void filterHidesRejectedEntries() throws IOException {
        Files.writeString(tempDir.resolve("keep.fx"), "");
        Files.writeString(tempDir.resolve("drop.txt"), "");

        var root = new FileNode(tempDir.toFile()).setValid(node -> node.getKey().getName().endsWith(".fx"));

        assertEquals(List.of("keep.fx"), namesOf(root));
    }

    @Test
    void repeatedListingReusesTheCachedResult() throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "");
        var root = new FileNode(tempDir.toFile());

        assertSame(root.getChildren(), root.getChildren());
    }

    @Test
    void addedEntryIsPickedUp() throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "");
        var root = new FileNode(tempDir.toFile());
        assertEquals(List.of("a.txt"), namesOf(root));

        Files.writeString(tempDir.resolve("b.txt"), "");
        touch(tempDir);

        assertEquals(List.of("a.txt", "b.txt"), namesOf(root));
    }

    @Test
    void removedEntryIsPickedUp() throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "");
        Files.writeString(tempDir.resolve("b.txt"), "");
        var root = new FileNode(tempDir.toFile());
        assertEquals(List.of("a.txt", "b.txt"), namesOf(root));

        Files.delete(tempDir.resolve("b.txt"));
        touch(tempDir);

        assertEquals(List.of("a.txt"), namesOf(root));
    }

    @Test
    void editingAnEntryDoesNotInvalidateTheListing() throws IOException {
        var file = Files.writeString(tempDir.resolve("a.txt"), "before");
        var root = new FileNode(tempDir.toFile());
        var children = root.getChildren();

        Files.writeString(file, "after");

        // the child set is what the tree diffs against, and a content edit does not touch it — so the
        // listing is not re-read either. Same instance, i.e. the write did not move the directory stamp.
        assertSame(children, root.getChildren());
    }

    /** As above, but with the filter arriving after a listing has already been cached. */
    @Test
    void changingTheFilterDropsTheCache() throws IOException {
        Files.writeString(tempDir.resolve("keep.fx"), "");
        Files.writeString(tempDir.resolve("drop.txt"), "");
        var root = new FileNode(tempDir.toFile());
        assertEquals(2, root.getChildren().size());

        root.setValid(node -> node.getKey().getName().endsWith(".fx"));

        assertEquals(List.of("keep.fx"), namesOf(root));
    }

    @Test
    void invalidateForcesAFreshListing() throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "");
        var root = new FileNode(tempDir.toFile());
        var children = root.getChildren();

        root.invalidate();

        assertNotSame(children, root.getChildren());
    }

    @Test
    void missingDirectoryListsAsEmpty() {
        assertEquals(List.of(), new FileNode(new File(tempDir.toFile(), "absent")).getChildren());
    }

    /**
     * The listing is revalidated against the directory's modification time, which a file system with a
     * coarse clock can fail to move. The cache ages out regardless, so the change still surfaces.
     */
    @Test
    void changeHiddenByAnUnmovedTimestampSurfacesOnceTheCacheAgesOut() throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "");
        var root = new FileNode(tempDir.toFile());
        assertEquals(List.of("a.txt"), namesOf(root));
        var stamp = Files.getLastModifiedTime(tempDir);

        Files.writeString(tempDir.resolve("b.txt"), "");
        Files.setLastModifiedTime(tempDir, stamp); // pretend the directory's clock never moved

        assertEquals(List.of("a.txt"), namesOf(root), "the stamp alone cannot see this change");

        Thread.sleep(300); // past MAX_CACHE_AGE_MS

        assertEquals(List.of("a.txt", "b.txt"), namesOf(root));
    }
}
