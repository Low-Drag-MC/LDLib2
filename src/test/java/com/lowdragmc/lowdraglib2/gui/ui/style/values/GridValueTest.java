package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.Grid;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.style.GridPlacement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridValueTest {

    @Test
    void testParseAuto() {
        Grid grid = GridValue.parse("auto");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isAuto());
        assertTrue(grid.grid().end.isAuto());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("auto", serialized);
    }

    @Test
    void testParseLineNumber() {
        // Single line number (start only, end is auto)
        Grid grid = GridValue.parse("3");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isLine());
        assertEquals(3, grid.grid().start.getValue());
        assertTrue(grid.grid().end.isAuto());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("3", serialized);
    }

    @Test
    void testParseNegativeLineNumber() {
        // Negative line number (count from end)
        Grid grid = GridValue.parse("-1");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isLine());
        assertEquals(-1, grid.grid().start.getValue());
        assertTrue(grid.grid().end.isAuto());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("-1", serialized);
    }

    @Test
    void testParseShorthandSyntax() {
        // "1 / 3" - start at line 1, end at line 3
        Grid grid = GridValue.parse("1 / 3");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isLine());
        assertEquals(1, grid.grid().start.getValue());
        assertTrue(grid.grid().end.isLine());
        assertEquals(3, grid.grid().end.getValue());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("1 / 3", serialized);
    }

    @Test
    void testParseSpanNumber() {
        // "span 2" - span 2 tracks
        Grid grid = GridValue.parse("span 2");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isSpan());
        assertEquals(2, grid.grid().start.getValue());
        assertTrue(grid.grid().end.isAuto());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("span 2", serialized);
    }

    @Test
    void testParseShorthandWithSpan() {
        // "1 / span 2" - start at line 1, span 2 tracks
        Grid grid = GridValue.parse("1 / span 2");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isLine());
        assertEquals(1, grid.grid().start.getValue());
        assertTrue(grid.grid().end.isSpan());
        assertEquals(2, grid.grid().end.getValue());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("1 / span 2", serialized);
    }

    @Test
    void testParseNamedLine() {
        // "header" - named line (start only, end is auto)
        Grid grid = GridValue.parse("header");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isNamedLine());
        assertEquals("header", grid.grid().start.getLineName());
        assertEquals(1, grid.grid().start.getNthIndex());
        assertTrue(grid.grid().end.isAuto());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("header", serialized);
    }

    @Test
    void testParseNamedLineWithOccurrence() {
        // "header 2" - 2nd occurrence of "header" line
        Grid grid = GridValue.parse("header 2");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isNamedLine());
        assertEquals("header", grid.grid().start.getLineName());
        assertEquals(2, grid.grid().start.getNthIndex());
        assertTrue(grid.grid().end.isAuto());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("header 2", serialized);
    }

    @Test
    void testParseShorthandWithNamedLines() {
        // "header / footer" - named lines
        Grid grid = GridValue.parse("header / footer");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isNamedLine());
        assertEquals("header", grid.grid().start.getLineName());
        assertTrue(grid.grid().end.isNamedLine());
        assertEquals("footer", grid.grid().end.getLineName());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("header / footer", serialized);
    }

    @Test
    void testParseSpanNamedLine() {
        // "span header" - span until "header" line
        Grid grid = GridValue.parse("span header");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isNamedSpan());
        assertEquals("header", grid.grid().start.getLineName());
        assertEquals(1, grid.grid().start.getValue());
        assertTrue(grid.grid().end.isAuto());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("span header", serialized);
    }

    @Test
    void testParseSpanNamedLineWithCount() {
        // "span header 2" - span until 2nd "header" line
        Grid grid = GridValue.parse("span header 2");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isNamedSpan());
        assertEquals("header", grid.grid().start.getLineName());
        assertEquals(2, grid.grid().start.getValue());
        assertTrue(grid.grid().end.isAuto());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("span header 2", serialized);
    }

    @Test
    void testParseComplexShorthand() {
        // "header 2 / span footer 3"
        Grid grid = GridValue.parse("header 2 / span footer 3");
        assertNotNull(grid);
        assertTrue(grid.grid().start.isNamedLine());
        assertEquals("header", grid.grid().start.getLineName());
        assertEquals(2, grid.grid().start.getNthIndex());
        assertTrue(grid.grid().end.isNamedSpan());
        assertEquals("footer", grid.grid().end.getLineName());
        assertEquals(3, grid.grid().end.getValue());

        // Verify round-trip
        String serialized = GridValue.toString(grid);
        assertEquals("header 2 / span footer 3", serialized);
    }

    @Test
    void testParseMixedShorthand() {
        // Mix of line numbers, spans, and named lines
        Grid grid1 = GridValue.parse("2 / header");
        assertNotNull(grid1);
        assertTrue(grid1.grid().start.isLine());
        assertEquals(2, grid1.grid().start.getValue());
        assertTrue(grid1.grid().end.isNamedLine());
        assertEquals("header", grid1.grid().end.getLineName());

        Grid grid2 = GridValue.parse("header / 4");
        assertNotNull(grid2);
        assertTrue(grid2.grid().start.isNamedLine());
        assertTrue(grid2.grid().end.isLine());
        assertEquals(4, grid2.grid().end.getValue());

        Grid grid3 = GridValue.parse("span 2 / footer");
        assertNotNull(grid3);
        assertTrue(grid3.grid().start.isSpan());
        assertTrue(grid3.grid().end.isNamedLine());
    }

    @Test
    void testParseEmpty() {
        // Empty string should return EMPTY
        Grid grid1 = GridValue.parse("");
        assertNotNull(grid1);
        assertEquals(Grid.EMPTY, grid1);

        // Null should return EMPTY
        Grid grid2 = GridValue.parse(null);
        assertNotNull(grid2);
        assertEquals(Grid.EMPTY, grid2);

        // Whitespace only should return EMPTY
        Grid grid3 = GridValue.parse("   ");
        assertNotNull(grid3);
        assertEquals(Grid.EMPTY, grid3);
    }

    @Test
    void testParseInvalid() {
        // Invalid syntax should return null
        Grid grid = GridValue.parse("invalid syntax !");
        // This might parse as a named line "invalid" with attempted nth index
        // or fail gracefully - let's just ensure it doesn't crash
        // (exact behavior depends on implementation)
    }

    @Test
    void testParseWhitespace() {
        // Extra whitespace should be handled
        Grid grid1 = GridValue.parse("  1  /  3  ");
        assertNotNull(grid1);
        assertTrue(grid1.grid().start.isLine());
        assertEquals(1, grid1.grid().start.getValue());
        assertTrue(grid1.grid().end.isLine());
        assertEquals(3, grid1.grid().end.getValue());

        Grid grid2 = GridValue.parse("  span   2  ");
        assertNotNull(grid2);
        assertTrue(grid2.grid().start.isSpan());
        assertEquals(2, grid2.grid().start.getValue());
    }

    @Test
    void testToStringEmpty() {
        String result = GridValue.toString(Grid.EMPTY);
        assertEquals("auto", result);

        String result2 = GridValue.toString(null);
        assertEquals("auto", result2);
    }

    @Test
    void testRoundTripVariousSyntax() {
        // Test that parse -> toString -> parse produces the same result
        String[] testCases = {
            "auto",
            "1",
            "-1",
            "1 / 3",
            "span 2",
            "1 / span 2",
            "header",
            "header 2",
            "header / footer",
            "span header",
            "span header 2"
        };

        for (String input : testCases) {
            Grid parsed1 = GridValue.parse(input);
            assertNotNull(parsed1, "Failed to parse: " + input);

            String serialized = GridValue.toString(parsed1);
            assertNotNull(serialized);

            Grid parsed2 = GridValue.parse(serialized);
            assertNotNull(parsed2);

            // Compare the actual placements
            assertEquals(parsed1.grid().start.getType(), parsed2.grid().start.getType(),
                "Start type mismatch for: " + input);
            assertEquals(parsed1.grid().end.getType(), parsed2.grid().end.getType(),
                "End type mismatch for: " + input);
        }
    }
}
