package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wire.WireRouteStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphViewPreferencesTest {

    /**
     * Touches the record before the store, on purpose.
     *
     * <p>A client loads them in this order — {@code GraphView}'s field initialisers read
     * {@code Entry.DEFAULTS} long before anything asks the store for anything — and the two used to
     * initialise each other, so the winner decided whether the map codec ended up holding a live
     * element codec or a {@code null} one. Entering from the store's side, as a test naturally does,
     * took the working path and saw nothing wrong while every write in the real client threw.</p>
     */
    @SuppressWarnings("unused")
    private static final GraphViewPreferences.Entry LOAD_ORDER_GUARD = GraphViewPreferences.Entry.DEFAULTS;

    private interface GraphA {
    }

    private interface GraphB {
    }

    @TempDir
    Path directory;
    private Path file;

    @BeforeEach
    void pointAtAScratchFile() {
        file = directory.resolve("graph_view.json");
        GraphViewPreferences.setFile(file);
    }

    @AfterEach
    void restore() {
        GraphViewPreferences.setFile(null);
    }

    /** Drops everything read so far, so the next read has to come off disk. */
    private void forgetWhatWasRead() {
        GraphViewPreferences.setFile(file);
    }

    private static GraphViewPreferences.Entry entry(WireRouteStyle style) {
        return new GraphViewPreferences.Entry(false, 24f, false, style);
    }

    @Test
    void anUnknownGraphTypeReadsTheDefaults() {
        assertEquals(GraphViewPreferences.Entry.DEFAULTS, GraphViewPreferences.get(GraphA.class));
    }

    @Test
    void whatWasPutComesBack() {
        var wanted = entry(WireRouteStyle.OCTILINEAR);
        GraphViewPreferences.put(GraphA.class, wanted);
        assertEquals(wanted, GraphViewPreferences.get(GraphA.class));
    }

    @Test
    void itReachesTheFileAndNotJustMemory() throws IOException {
        GraphViewPreferences.put(GraphA.class, entry(WireRouteStyle.CURVED));
        assertTrue(Files.exists(file), "a change should be written straight away, not on close");
        forgetWhatWasRead();
        assertEquals(entry(WireRouteStyle.CURVED), GraphViewPreferences.get(GraphA.class));
    }

    @Test
    void graphTypesDoNotShareASetup() {
        GraphViewPreferences.put(GraphA.class, entry(WireRouteStyle.OCTILINEAR));
        GraphViewPreferences.put(GraphB.class, entry(WireRouteStyle.STRAIGHT));
        forgetWhatWasRead();
        assertEquals(WireRouteStyle.OCTILINEAR, GraphViewPreferences.get(GraphA.class).wireStyle());
        assertEquals(WireRouteStyle.STRAIGHT, GraphViewPreferences.get(GraphB.class).wireStyle());
    }

    @Test
    void writingOneTypeLeavesTheRestOfTheFileAlone() {
        GraphViewPreferences.put(GraphA.class, entry(WireRouteStyle.OCTILINEAR));
        forgetWhatWasRead();
        // A fresh read followed by a write of a different type: the type that was not touched has
        // to survive, which it only does if the write merges rather than replaces.
        GraphViewPreferences.put(GraphB.class, entry(WireRouteStyle.STRAIGHT));
        forgetWhatWasRead();
        assertEquals(WireRouteStyle.OCTILINEAR, GraphViewPreferences.get(GraphA.class).wireStyle());
    }

    @Test
    void puttingTheSameSetupTwiceDoesNotRewriteTheFile() throws IOException {
        var wanted = entry(WireRouteStyle.CURVED);
        GraphViewPreferences.put(GraphA.class, wanted);
        Files.delete(file);
        GraphViewPreferences.put(GraphA.class, wanted);
        assertFalse(Files.exists(file), "nothing changed, so nothing should have been written");
    }

    @Test
    void everyWireStyleSurvivesARoundTrip() {
        for (var style : WireRouteStyle.values()) {
            GraphViewPreferences.put(GraphA.class, entry(style));
            forgetWhatWasRead();
            assertEquals(style, GraphViewPreferences.get(GraphA.class).wireStyle(), style.name());
        }
    }

    @Test
    void aStyleThatNoLongerExistsReadsAsTheDefault() throws IOException {
        // What a file written by a later build looks like to this one. It must not take the rest of
        // the entry — or the rest of the file — down with it.
        Files.writeString(file, """
                {"%s": {"snap_to_grid": false, "grid_size": 24.0, "snap_to_elements": false,
                        "wire_style": "SOMETHING_FROM_THE_FUTURE"}}
                """.formatted(GraphA.class.getName()));
        forgetWhatWasRead();
        var entry = GraphViewPreferences.get(GraphA.class);
        assertEquals(WireRouteStyle.DEFAULT, entry.wireStyle());
        assertFalse(entry.snapToGrid(), "the fields that did parse should still be honoured");
        assertEquals(24f, entry.gridSnapSize());
    }

    @Test
    void fieldsMissingFromAnOlderFileFallBackToTheDefaults() throws IOException {
        Files.writeString(file, "{\"%s\": {\"wire_style\": \"ORTHOGONAL\"}}".formatted(GraphA.class.getName()));
        forgetWhatWasRead();
        var entry = GraphViewPreferences.get(GraphA.class);
        assertEquals(WireRouteStyle.ORTHOGONAL, entry.wireStyle());
        assertEquals(GraphViewPreferences.Entry.DEFAULTS.snapToGrid(), entry.snapToGrid());
        assertEquals(GraphViewPreferences.Entry.DEFAULTS.gridSnapSize(), entry.gridSnapSize());
    }

    @Test
    void anUnreadableFileIsIgnoredRatherThanThrown() throws IOException {
        // Opening an editor must never fail because a preferences file got mangled.
        Files.writeString(file, "this is not json {{{");
        forgetWhatWasRead();
        assertEquals(GraphViewPreferences.Entry.DEFAULTS, GraphViewPreferences.get(GraphA.class));
    }

    @Test
    void aMissingFileIsSimplyTheDefaults() {
        GraphViewPreferences.setFile(directory.resolve("nested").resolve("nope.json"));
        assertEquals(GraphViewPreferences.Entry.DEFAULTS, GraphViewPreferences.get(GraphA.class));
    }

    @Test
    void theDirectoryIsCreatedOnDemand() {
        var nested = directory.resolve("deep").resolve("deeper").resolve("graph_view.json");
        GraphViewPreferences.setFile(nested);
        GraphViewPreferences.put(GraphA.class, entry(WireRouteStyle.CURVED));
        assertTrue(Files.exists(nested));
    }

    @Test
    void theKeyIsTheGraphClassName() {
        assertEquals(GraphA.class.getName(), GraphViewPreferences.keyOf(GraphA.class));
    }
}
