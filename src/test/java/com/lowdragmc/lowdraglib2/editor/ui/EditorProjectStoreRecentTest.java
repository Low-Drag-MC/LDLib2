package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.ui.EditorProjectStore.RecentEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The recent projects list is kept per project type: two editors sharing the store file must not see,
 * evict or clear each other's projects.
 */
class EditorProjectStoreRecentTest {
    private static final ProjectType UI = ProjectType.of("ui", ".ui", () -> null);
    private static final ProjectType GRAPH = ProjectType.of("graph", ".graph", () -> null);

    private static RecentEntry entry(String path, ProjectType type) {
        return new RecentEntry(path, type.getName());
    }

    @Test
    void listingOnlyOffersTheAskingEditorsProjectTypes() {
        var ui = entry("./a.ui", UI);
        var graph = entry("./b.graph", GRAPH);
        var entries = List.of(graph, ui);

        assertEquals(List.of(ui), EditorProjectStore.selectRecentProjects(entries, List.of(UI)));
        assertEquals(List.of(graph), EditorProjectStore.selectRecentProjects(entries, List.of(GRAPH)));
        // an editor that opens both sees both, still newest first
        assertEquals(List.of(graph, ui), EditorProjectStore.selectRecentProjects(entries, List.of(UI, GRAPH)));
    }

    @Test
    void anEditorWithoutProjectTypesListsNothing() {
        assertTrue(EditorProjectStore.selectRecentProjects(List.of(entry("./a.ui", UI)), List.of()).isEmpty());
    }

    @Test
    void addingPutsTheProjectFirstAndDropsItsOlderMention() {
        var first = entry("./a.ui", UI);
        var second = entry("./b.ui", UI);

        var updated = EditorProjectStore.addRecentProject(List.of(first, second), second.path(), UI, 5);

        assertEquals(List.of(second, first), updated);
    }

    @Test
    void addingNeverEvictsAnotherProjectTypesEntries() {
        var graphs = List.of(entry("./a.graph", GRAPH), entry("./b.graph", GRAPH));
        var entries = new ArrayList<>(graphs);
        entries.add(entry("./old.ui", UI));

        // a limit of one keeps a single ui project, and says nothing about the graph editor's list
        var updated = EditorProjectStore.addRecentProject(entries, "./new.ui", UI, 1);

        assertEquals(List.of(entry("./new.ui", UI), graphs.get(0), graphs.get(1)), updated);
    }

    @Test
    void aLimitOfZeroForgetsOnlyItsOwnProjectType() {
        var graph = entry("./a.graph", GRAPH);
        var entries = List.of(entry("./a.ui", UI), graph);

        var updated = EditorProjectStore.addRecentProject(entries, "./b.ui", UI, 0);

        assertEquals(List.of(graph), updated);
    }

    @Test
    void clearingForgetsOnlyTheGivenProjectTypes() {
        var graph = entry("./a.graph", GRAPH);
        var entries = List.of(entry("./a.ui", UI), graph);

        assertEquals(List.of(graph), EditorProjectStore.clearRecentProjects(entries, List.of(UI)));
        assertEquals(List.of(), EditorProjectStore.clearRecentProjects(entries, List.of(UI, GRAPH)));
    }

    @Test
    void entriesSurviveTheNbtRoundTrip() {
        var entries = List.of(entry("./a.ui", UI), entry("./b.graph", GRAPH));
        var tag = new CompoundTag();

        EditorProjectStore.writeEntries(tag, entries);

        assertEquals(entries, EditorProjectStore.readEntries(tag));
    }

    /**
     * A list written before recents were kept per type holds bare paths. Those must not vanish, and must
     * end up with the editor whose files they are.
     */
    @Test
    void legacyEntriesAreClaimedByTheTypeThatRecognisesTheFile() {
        var legacy = new ListTag();
        legacy.add(StringTag.valueOf("./a.ui"));
        legacy.add(StringTag.valueOf("./a.graph"));
        var tag = new CompoundTag();
        tag.put("recent", legacy);

        var entries = EditorProjectStore.readEntries(tag);

        assertEquals(List.of(new RecentEntry("./a.ui", ""), new RecentEntry("./a.graph", "")), entries);
        assertEquals(List.of(entries.get(0)), EditorProjectStore.selectRecentProjects(entries, List.of(UI)));
        assertEquals(List.of(entries.get(1)), EditorProjectStore.selectRecentProjects(entries, List.of(GRAPH)));
    }

    /** Re-opening a legacy entry gives it a type, rather than leaving a second, untyped copy behind. */
    @Test
    void addingReplacesTheLegacyEntryOfTheSameProject() {
        var entries = List.of(new RecentEntry("./a.ui", ""));

        var updated = EditorProjectStore.addRecentProject(entries, "./a.ui", UI, 5);

        assertEquals(List.of(entry("./a.ui", UI)), updated);
    }

    /** A legacy entry the asking editor cannot open is left where it is, for the editor that can. */
    @Test
    void clearingLeavesLegacyEntriesOfOtherProjectTypes() {
        var entries = List.of(new RecentEntry("./a.ui", ""), new RecentEntry("./a.graph", ""));

        assertEquals(List.of(entries.get(1)), EditorProjectStore.clearRecentProjects(entries, List.of(UI)));
    }
}
