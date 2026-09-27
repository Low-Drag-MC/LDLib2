package com.lowdragmc.lowdraglib2.editor.ui.browser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetSearchTest {
    /** Two types where one extension ends with the other, as in the real set of resource types. */
    private static final List<String> EXTENSIONS = List.of(".graph.nbt", ".test_graph.nbt", ".texture.nbt");

    @TempDir
    Path tempDir;

    @Test
    void displayNameDropsTheLongestResourceExtension() {
        assertEquals("stone", AssetSearch.displayName("stone.texture.nbt", EXTENSIONS));
        // .graph.nbt matches too, but the longer .test_graph.nbt is what the type resolves to
        assertEquals("flow", AssetSearch.displayName("flow.test_graph.nbt", EXTENSIONS));
        assertEquals("notes.txt", AssetSearch.displayName("notes.txt", EXTENSIONS));
        // nothing but an extension is not a resource, so it keeps its whole name
        assertEquals(".texture.nbt", AssetSearch.displayName(".texture.nbt", EXTENSIONS));
    }

    @Test
    void matchesTheShownNameIgnoringCase() {
        assertTrue(AssetSearch.matches("Stone_Bricks.texture.nbt", false, "brick", EXTENSIONS));
        // the extension is not part of the shown name, so it cannot match every resource of the type
        assertFalse(AssetSearch.matches("stone.texture.nbt", false, "texture", EXTENSIONS));
        // a folder is shown with its whole name, dots and all
        assertTrue(AssetSearch.matches("old.texture.nbt", true, "texture", EXTENSIONS));
        assertTrue(AssetSearch.matches("anything", false, "", EXTENSIONS));
    }

    @Test
    void walksTheWholeTreeButNotTheRoot() throws IOException {
        var root = tempDir.resolve("stone");
        Files.createDirectories(root.resolve("blocks/stone_variants"));
        Files.createDirectories(root.resolve("items"));
        Files.writeString(root.resolve("blocks/stone.texture.nbt"), "");
        Files.writeString(root.resolve("blocks/stone_variants/mossy_stone.texture.nbt"), "");
        Files.writeString(root.resolve("items/stone_sword.graph.nbt"), "");
        Files.writeString(root.resolve("items/dirt.texture.nbt"), "");

        var search = AssetSearch.runNow(root.toFile(), "Stone", EXTENSIONS, 100);

        assertTrue(search.isFinished());
        assertFalse(search.isTruncated());
        assertEquals(Set.of("stone_variants", "stone.texture.nbt", "mossy_stone.texture.nbt", "stone_sword.graph.nbt"),
                names(drain(search)));
        assertEquals(4, search.getFound());
    }

    @Test
    void hitsCarryTheAttributesOfTheWalk() throws IOException {
        Files.createDirectories(tempDir.resolve("folder_a"));
        Files.writeString(tempDir.resolve("file_a.txt"), "12345");

        var hits = drain(AssetSearch.runNow(tempDir.toFile(), "_a", EXTENSIONS, 100));

        var folder = hits.stream().filter(hit -> hit.file().getName().equals("folder_a")).findFirst().orElseThrow();
        var file = hits.stream().filter(hit -> hit.file().getName().equals("file_a.txt")).findFirst().orElseThrow();
        assertTrue(folder.directory());
        assertFalse(file.directory());
        assertEquals(5, file.size());
        assertEquals(file.file().lastModified(), file.lastModified());
    }

    @Test
    void stopsAtTheLimit() throws IOException {
        for (var i = 0; i < 20; i++) {
            Files.writeString(tempDir.resolve("match_" + i + ".txt"), "");
        }

        var search = AssetSearch.runNow(tempDir.toFile(), "match", EXTENSIONS, 5);

        assertTrue(search.isFinished());
        assertTrue(search.isTruncated());
        assertEquals(5, drain(search).size());
    }

    @Test
    void runsOffTheCallingThreadAndFindsTheSame() throws IOException, InterruptedException {
        Files.createDirectories(tempDir.resolve("a/b/c"));
        Files.writeString(tempDir.resolve("a/b/c/deep_match.txt"), "");
        Files.writeString(tempDir.resolve("a/shallow_match.txt"), "");

        var search = AssetSearch.start(tempDir.toFile(), "match", EXTENSIONS, 100);
        awaitFinished(search);

        assertEquals(Set.of("deep_match.txt", "shallow_match.txt"), names(drain(search)));
    }

    @Test
    void aCancelledSearchStillFinishes() throws IOException, InterruptedException {
        for (var i = 0; i < 50; i++) {
            Files.createDirectories(tempDir.resolve("dir_" + i + "/inner"));
        }

        var search = AssetSearch.start(tempDir.toFile(), "dir", EXTENSIONS, 1000);
        search.cancel();
        awaitFinished(search);

        assertTrue(search.isCancelled());
        assertFalse(search.isTruncated());
        assertTrue(search.getFound() <= 50);
    }

    private static void awaitFinished(AssetSearch search) throws InterruptedException {
        var deadline = System.currentTimeMillis() + 10_000;
        while (!search.isFinished()) {
            if (System.currentTimeMillis() > deadline) throw new AssertionError("the search never finished");
            Thread.sleep(5);
        }
    }

    private static List<AssetSearch.Hit> drain(AssetSearch search) {
        var hits = new ArrayList<AssetSearch.Hit>();
        for (var hit = search.poll(); hit != null; hit = search.poll()) {
            hits.add(hit);
        }
        return hits;
    }

    private static Set<String> names(List<AssetSearch.Hit> hits) {
        return hits.stream().map(AssetSearch.Hit::file).map(File::getName).collect(Collectors.toSet());
    }
}
