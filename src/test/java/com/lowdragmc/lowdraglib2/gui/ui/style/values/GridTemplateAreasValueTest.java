package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplateAreas;
import dev.vfyjxf.taffy.style.GridTemplateArea;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridTemplateAreasValueTest {

    @Test
    void testParseSingleRow() {
        // Single row with header spanning 3 columns
        GridTemplateAreas areas = GridTemplateAreasValue.parse("\"header header header\"");
        assertNotNull(areas);
        assertEquals(1, areas.areas().size());

        GridTemplateArea header = areas.areas().get(0);
        assertEquals("header", header.getName());
        assertEquals(1, header.getRowStart());
        assertEquals(2, header.getRowEnd());
        assertEquals(1, header.getColumnStart());
        assertEquals(4, header.getColumnEnd());

        // Verify round-trip
        String serialized = GridTemplateAreasValue.toString(areas);
        assertEquals("\"header header header\"", serialized);
    }

    @Test
    void testParseMultipleRows() {
        // Multiple rows with different areas
        String input = "\"header header header\" \"nav main aside\" \"footer footer footer\"";
        GridTemplateAreas areas = GridTemplateAreasValue.parse(input);
        assertNotNull(areas);
        assertEquals(5, areas.areas().size()); // header, nav, main, aside, footer

        // Find and verify each area
        GridTemplateArea header = findArea(areas, "header");
        assertNotNull(header);
        assertEquals(1, header.getRowStart());
        assertEquals(2, header.getRowEnd());
        assertEquals(1, header.getColumnStart());
        assertEquals(4, header.getColumnEnd());

        GridTemplateArea nav = findArea(areas, "nav");
        assertNotNull(nav);
        assertEquals(2, nav.getRowStart());
        assertEquals(3, nav.getRowEnd());
        assertEquals(1, nav.getColumnStart());
        assertEquals(2, nav.getColumnEnd());

        GridTemplateArea main = findArea(areas, "main");
        assertNotNull(main);
        assertEquals(2, main.getRowStart());
        assertEquals(3, main.getRowEnd());
        assertEquals(2, main.getColumnStart());
        assertEquals(3, main.getColumnEnd());

        // Verify round-trip
        String serialized = GridTemplateAreasValue.toString(areas);
        GridTemplateAreas reparsed = GridTemplateAreasValue.parse(serialized);
        assertNotNull(reparsed);
        assertEquals(areas.areas().size(), reparsed.areas().size());
    }

    @Test
    void testParseEmptyCells() {
        // Using dots for empty cells
        GridTemplateAreas areas = GridTemplateAreasValue.parse("\"header header .\" \"nav main aside\"");
        assertNotNull(areas);
        // Only 4 named areas (header, nav, main, aside) - dots are skipped
        assertEquals(4, areas.areas().size());

        GridTemplateArea header = findArea(areas, "header");
        assertNotNull(header);
        assertEquals(1, header.getRowStart());
        assertEquals(2, header.getRowEnd());
        assertEquals(1, header.getColumnStart());
        assertEquals(3, header.getColumnEnd()); // Spans columns 1-2

        // Verify round-trip preserves empty cell
        String serialized = GridTemplateAreasValue.toString(areas);
        assertTrue(serialized.contains("."));
    }

    @Test
    void testParseEmpty() {
        // Test empty string
        GridTemplateAreas areas = GridTemplateAreasValue.parse("");
        assertNotNull(areas);
        assertEquals(GridTemplateAreas.EMPTY, areas);

        // Test null
        GridTemplateAreas areas2 = GridTemplateAreasValue.parse(null);
        assertNotNull(areas2);
        assertEquals(GridTemplateAreas.EMPTY, areas2);
    }

    @Test
    void testParseInvalidUnequalColumns() {
        // Invalid: rows have different number of columns
        GridTemplateAreas areas = GridTemplateAreasValue.parse("\"header header\" \"nav main aside\"");
        // Should return null because rows don't have same column count
        assertNull(areas);
    }

    @Test
    void testParseMultilineFormat() {
        // Test with actual newlines (like in CSS)
        String input = """
                "header header header"
                "nav main aside"
                "footer footer footer"
                """;
        GridTemplateAreas areas = GridTemplateAreasValue.parse(input);
        assertNotNull(areas);
        assertEquals(5, areas.areas().size());

        assertNotNull(findArea(areas, "header"));
        assertNotNull(findArea(areas, "nav"));
        assertNotNull(findArea(areas, "footer"));
    }

    @Test
    void testParseSingleCellPerRow() {
        // Single cell per row
        GridTemplateAreas areas = GridTemplateAreasValue.parse("\"header\" \"main\" \"footer\"");
        assertNotNull(areas);
        assertEquals(3, areas.areas().size());

        GridTemplateArea header = findArea(areas, "header");
        assertEquals(1, header.getRowStart());
        assertEquals(2, header.getRowEnd());
        assertEquals(1, header.getColumnStart());
        assertEquals(2, header.getColumnEnd());

        GridTemplateArea main = findArea(areas, "main");
        assertEquals(2, main.getRowStart());
        assertEquals(3, main.getRowEnd());

        GridTemplateArea footer = findArea(areas, "footer");
        assertEquals(3, footer.getRowStart());
        assertEquals(4, footer.getRowEnd());

        // Verify round-trip
        String serialized = GridTemplateAreasValue.toString(areas);
        assertEquals("\"header\" \"main\" \"footer\"", serialized);
    }

    @Test
    void testParseComplexLayout() {
        // More complex layout with repeated area names
        String input = "\"header header header header\" \"sidebar content content aside\" \"sidebar footer footer footer\"";
        GridTemplateAreas areas = GridTemplateAreasValue.parse(input);
        assertNotNull(areas);
        assertEquals(5, areas.areas().size()); // header, sidebar, content, aside, footer

        // Check that sidebar spans two rows
        GridTemplateArea sidebar = findArea(areas, "sidebar");
        assertNotNull(sidebar);
        assertEquals(2, sidebar.getRowStart());
        assertEquals(4, sidebar.getRowEnd()); // Rows 2-3
        assertEquals(1, sidebar.getColumnStart());
        assertEquals(2, sidebar.getColumnEnd()); // Column 1

        // Check that content spans one row, two columns
        GridTemplateArea content = findArea(areas, "content");
        assertNotNull(content);
        assertEquals(2, content.getRowStart());
        assertEquals(3, content.getRowEnd()); // Row 2
        assertEquals(2, content.getColumnStart());
        assertEquals(4, content.getColumnEnd()); // Columns 2-3

        // Verify round-trip
        String serialized = GridTemplateAreasValue.toString(areas);
        GridTemplateAreas reparsed = GridTemplateAreasValue.parse(serialized);
        assertEquals(areas.areas().size(), reparsed.areas().size());
    }

    // Helper method to find an area by name
    private GridTemplateArea findArea(GridTemplateAreas areas, String name) {
        return areas.areas().stream()
            .filter(area -> area.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
}
