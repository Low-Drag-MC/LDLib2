package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.GridAuto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridAutoValueTest {

    @Test
    void testParseSingleTrack() {
        // Single track value
        GridAuto gridAuto = GridAutoValue.parse("100px");
        assertNotNull(gridAuto);
        assertEquals(1, gridAuto.values().size());

        // Verify round-trip
        String serialized = GridAutoValue.toString(gridAuto);
        assertEquals("100px", serialized);
    }

    @Test
    void testParseMultipleTracks() {
        // Multiple tracks
        GridAuto gridAuto = GridAutoValue.parse("100px 1fr auto");
        assertNotNull(gridAuto);
        assertEquals(3, gridAuto.values().size());

        // Verify round-trip
        String serialized = GridAutoValue.toString(gridAuto);
        assertEquals("100px 1fr auto", serialized);
    }

    @Test
    void testParseKeywords() {
        // Test various keywords
        GridAuto gridAuto = GridAutoValue.parse("auto min-content max-content");
        assertNotNull(gridAuto);
        assertEquals(3, gridAuto.values().size());

        // Verify round-trip
        String serialized = GridAutoValue.toString(gridAuto);
        assertEquals("auto min-content max-content", serialized);
    }

    @Test
    void testParseFlexValues() {
        // Flex values
        GridAuto gridAuto = GridAutoValue.parse("1fr 2fr 3fr");
        assertNotNull(gridAuto);
        assertEquals(3, gridAuto.values().size());

        // Verify round-trip
        String serialized = GridAutoValue.toString(gridAuto);
        assertEquals("1fr 2fr 3fr", serialized);
    }

    @Test
    void testParseMinmax() {
        // Minmax function
        GridAuto gridAuto = GridAutoValue.parse("minmax(100px, 1fr)");
        assertNotNull(gridAuto);
        assertEquals(1, gridAuto.values().size());

        // Verify round-trip
        String serialized = GridAutoValue.toString(gridAuto);
        assertEquals("minmax(100px, 1fr)", serialized);
    }

    @Test
    void testParseFitContent() {
        // Fit-content function
        GridAuto gridAuto = GridAutoValue.parse("fit-content(200px)");
        assertNotNull(gridAuto);
        assertEquals(1, gridAuto.values().size());

        // Verify round-trip
        String serialized = GridAutoValue.toString(gridAuto);
        assertEquals("fit-content(200px)", serialized);
    }

    @Test
    void testParsePercentage() {
        // Percentage values
        GridAuto gridAuto = GridAutoValue.parse("50% 25%");
        assertNotNull(gridAuto);
        assertEquals(2, gridAuto.values().size());

        // Verify round-trip
        String serialized = GridAutoValue.toString(gridAuto);
        assertEquals("50% 25%", serialized);
    }

    @Test
    void testParseEmpty() {
        // Test empty string
        GridAuto gridAuto = GridAutoValue.parse("");
        assertNotNull(gridAuto);
        assertEquals(GridAuto.EMPTY, gridAuto);

        // Test null
        GridAuto gridAuto2 = GridAutoValue.parse(null);
        assertNotNull(gridAuto2);
        assertEquals(GridAuto.EMPTY, gridAuto2);
    }

    @Test
    void testParseComplexCombination() {
        // Complex combination
        GridAuto gridAuto = GridAutoValue.parse("minmax(100px, 1fr) auto fit-content(300px)");
        assertNotNull(gridAuto);
        assertEquals(3, gridAuto.values().size());

        // Verify round-trip
        String serialized = GridAutoValue.toString(gridAuto);
        assertEquals("minmax(100px, 1fr) auto fit-content(300px)", serialized);
    }

    @Test
    void testParseInvalidSkipsToken() {
        // Invalid tokens are skipped
        GridAuto gridAuto = GridAutoValue.parse("100px invalid 1fr");
        assertNotNull(gridAuto);
        // Only valid tokens are parsed
        assertEquals(2, gridAuto.values().size());
    }
}
