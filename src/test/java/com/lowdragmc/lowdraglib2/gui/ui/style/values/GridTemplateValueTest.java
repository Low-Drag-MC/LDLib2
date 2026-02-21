package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridTemplateValueTest {

    @Test
    void testParseSimpleTracks() {
        // Example 1: Simple tracks
        GridTemplate template = GridTemplateValue.parse("100px 1fr 2fr");
        assertNotNull(template);
        assertEquals(3, template.simples().size());
        assertEquals(3, template.repeats().size());
        assertEquals(0, template.names().size());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("100px 1fr 2fr", serialized);
    }

    @Test
    void testParseNamedLines() {
        // Example 2: Named lines
        GridTemplate template = GridTemplateValue.parse("[start] 100px [middle] 1fr [end]");
        assertNotNull(template);
        assertEquals(2, template.simples().size());
        assertEquals(2, template.repeats().size());
        assertEquals(3, template.names().size());
        assertEquals("start", template.names().getFirst().getName());
        assertEquals(0, template.names().getFirst().getIndex());
        assertEquals("middle", template.names().get(1).getName());
        assertEquals(1, template.names().get(1).getIndex());
        assertEquals("end", template.names().get(2).getName());
        assertEquals(2, template.names().get(2).getIndex());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("[start] 100px [middle] 1fr [end]", serialized);
    }

    @Test
    void testParseRepeatFunction() {
        // Example 3: Repeat function with count
        GridTemplate template = GridTemplateValue.parse("repeat(3, 1fr)");
        assertNotNull(template);
        assertEquals(0, template.simples().size());
        assertEquals(1, template.repeats().size());
        assertTrue(template.repeats().getFirst().isRepeat());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("repeat(3, 1fr)", serialized);
    }

    @Test
    void testParseComplexCombination() {
        // Example 4: Complex combination
        GridTemplate template = GridTemplateValue.parse("[start] 100px repeat(2, 1fr) [end]");
        assertNotNull(template);
        assertEquals(1, template.simples().size());
        assertEquals(2, template.repeats().size());
        assertEquals(2, template.names().size());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("[start] 100px repeat(2, 1fr) [end]", serialized);
    }

    @Test
    void testParseAutoFillRepeat() {
        // Test auto-fill repeat
        GridTemplate template = GridTemplateValue.parse("repeat(auto-fill, minmax(100px, 1fr))");
        assertNotNull(template);
        assertEquals(1, template.repeats().size());
        assertTrue(template.repeats().getFirst().isRepeat());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("repeat(auto-fill, minmax(100px, 1fr))", serialized);
    }

    @Test
    void testParseAutoFitRepeat() {
        // Test auto-fit repeat
        GridTemplate template = GridTemplateValue.parse("repeat(auto-fit, 200px)");
        assertNotNull(template);
        assertEquals(1, template.repeats().size());
        assertTrue(template.repeats().getFirst().isRepeat());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("repeat(auto-fit, 200px)", serialized);
    }

    @Test
    void testParseKeywords() {
        // Test various keywords
        GridTemplate template = GridTemplateValue.parse("auto min-content max-content");
        assertNotNull(template);
        assertEquals(3, template.simples().size());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("auto min-content max-content", serialized);
    }

    @Test
    void testParseFitContent() {
        // Test fit-content function
        GridTemplate template = GridTemplateValue.parse("fit-content(200px)");
        assertNotNull(template);
        assertEquals(1, template.simples().size());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("fit-content(200px)", serialized);
    }

    @Test
    void testParseMinmax() {
        // Test minmax function
        GridTemplate template = GridTemplateValue.parse("minmax(100px, 1fr)");
        assertNotNull(template);
        assertEquals(1, template.simples().size());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("minmax(100px, 1fr)", serialized);
    }

    @Test
    void testParsePercentage() {
        // Test percentage values
        GridTemplate template = GridTemplateValue.parse("50% 25%");
        assertNotNull(template);
        assertEquals(2, template.simples().size());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("50% 25%", serialized);
    }

    @Test
    void testParseEmpty() {
        // Test empty string
        GridTemplate template = GridTemplateValue.parse("");
        assertNotNull(template);
        assertEquals(GridTemplate.EMPTY, template);

        // Test null
        GridTemplate template2 = GridTemplateValue.parse(null);
        assertNotNull(template2);
        assertEquals(GridTemplate.EMPTY, template2);
    }

    @Test
    void testParseInvalidSyntax() {
        // Test invalid syntax - parser is lenient and skips invalid tokens
        // This follows CSS behavior where invalid parts are ignored
        GridTemplate template = GridTemplateValue.parse("invalid(syntax");
        assertNull(template);
    }

    @Test
    void testParseNestedMinmax() {
        // Test nested function: repeat with minmax
        GridTemplate template = GridTemplateValue.parse("repeat(3, minmax(100px, 1fr))");
        assertNotNull(template);
        assertEquals(1, template.repeats().size());

        // Verify round-trip
        String serialized = GridTemplateValue.toString(template);
        assertEquals("repeat(3, minmax(100px, 1fr))", serialized);
    }
}
