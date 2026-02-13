package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import dev.vfyjxf.taffy.style.TaffyDimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DimensionValueTest {

    @Test
    void testParseAuto() {
        TaffyDimension dim = DimensionValue.parse("auto");
        assertNotNull(dim);
        assertTrue(dim.isAuto());

        // Verify round-trip
        String serialized = DimensionValue.toString(dim);
        assertEquals("auto", serialized);
    }

    @Test
    void testParseLength() {
        // With "px" unit
        TaffyDimension dim1 = DimensionValue.parse("100px");
        assertNotNull(dim1);
        assertTrue(dim1.isLength());
        assertEquals(100f, dim1.getValue(), 0.001f);

        // Without unit (assume px)
        TaffyDimension dim2 = DimensionValue.parse("50");
        assertNotNull(dim2);
        assertTrue(dim2.isLength());
        assertEquals(50f, dim2.getValue(), 0.001f);

        // Negative length
        TaffyDimension dim3 = DimensionValue.parse("-25px");
        assertNotNull(dim3);
        assertTrue(dim3.isLength());
        assertEquals(-25f, dim3.getValue(), 0.001f);

        // Verify round-trip
        assertEquals("100.0px", DimensionValue.toString(dim1));
        assertEquals("50.0px", DimensionValue.toString(dim2));
        assertEquals("-25.0px", DimensionValue.toString(dim3));
    }

    @Test
    void testParsePercentage() {
        TaffyDimension dim = DimensionValue.parse("50%");
        assertNotNull(dim);
        assertTrue(dim.isPercent());
        // CSS percentages are 0-100, internally 0.0-1.0
        assertEquals(0.5f, dim.getValue(), 0.001f);

        // Verify round-trip
        String serialized = DimensionValue.toString(dim);
        assertEquals("50.0%", serialized);
    }

    @Test
    void testParseZero() {
        TaffyDimension dim1 = DimensionValue.parse("0");
        assertNotNull(dim1);
        assertTrue(dim1.isLength());
        assertEquals(0f, dim1.getValue(), 0.001f);

        TaffyDimension dim2 = DimensionValue.parse("0px");
        assertNotNull(dim2);
        assertTrue(dim2.isLength());
        assertEquals(0f, dim2.getValue(), 0.001f);

        TaffyDimension dim3 = DimensionValue.parse("0%");
        assertNotNull(dim3);
        assertTrue(dim3.isPercent());
        assertEquals(0f, dim3.getValue(), 0.001f);
    }

    @Test
    void testParseCaseInsensitive() {
        TaffyDimension dim1 = DimensionValue.parse("AUTO");
        assertTrue(dim1.isAuto());

        TaffyDimension dim2 = DimensionValue.parse("100PX");
        assertTrue(dim2.isLength());
        assertEquals(100f, dim2.getValue(), 0.001f);

        TaffyDimension dim3 = DimensionValue.parse("50%");
        assertTrue(dim3.isPercent());
        assertEquals(0.5f, dim3.getValue(), 0.001f);
    }

    @Test
    void testParseWhitespace() {
        // Extra whitespace should be handled
        TaffyDimension dim1 = DimensionValue.parse("  100px  ");
        assertNotNull(dim1);
        assertTrue(dim1.isLength());
        assertEquals(100f, dim1.getValue(), 0.001f);

        TaffyDimension dim2 = DimensionValue.parse("  auto  ");
        assertNotNull(dim2);
        assertTrue(dim2.isAuto());
    }

    @Test
    void testParseEmpty() {
        // Empty string should return auto
        TaffyDimension dim1 = DimensionValue.parse("");
        assertNotNull(dim1);
        assertTrue(dim1.isAuto());

        // Null should return auto
        TaffyDimension dim2 = DimensionValue.parse(null);
        assertNotNull(dim2);
        assertTrue(dim2.isAuto());
    }

    @Test
    void testParseInvalid() {
        // Invalid syntax should return null
        TaffyDimension dim = DimensionValue.parse("invalid");
        assertNull(dim);
    }

    @Test
    void testParseFloatingPoint() {
        // Floating point lengths
        TaffyDimension dim1 = DimensionValue.parse("12.5px");
        assertNotNull(dim1);
        assertTrue(dim1.isLength());
        assertEquals(12.5f, dim1.getValue(), 0.001f);

        // Floating point percentages
        TaffyDimension dim2 = DimensionValue.parse("33.33%");
        assertNotNull(dim2);
        assertTrue(dim2.isPercent());
        assertEquals(0.3333f, dim2.getValue(), 0.001f);
    }

    @Test
    void testParseHundredPercent() {
        TaffyDimension dim = DimensionValue.parse("100%");
        assertNotNull(dim);
        assertTrue(dim.isPercent());
        assertEquals(1.0f, dim.getValue(), 0.001f);

        // Verify round-trip
        String serialized = DimensionValue.toString(dim);
        assertEquals("100.0%", serialized);
    }

    @Test
    void testParseSmallValues() {
        TaffyDimension dim1 = DimensionValue.parse("0.5px");
        assertNotNull(dim1);
        assertTrue(dim1.isLength());
        assertEquals(0.5f, dim1.getValue(), 0.001f);

        TaffyDimension dim2 = DimensionValue.parse("0.1%");
        assertNotNull(dim2);
        assertTrue(dim2.isPercent());
        assertEquals(0.001f, dim2.getValue(), 0.0001f);
    }

    @Test
    void testParseLargeValues() {
        TaffyDimension dim1 = DimensionValue.parse("9999px");
        assertNotNull(dim1);
        assertTrue(dim1.isLength());
        assertEquals(9999f, dim1.getValue(), 0.001f);

        TaffyDimension dim2 = DimensionValue.parse("200%");
        assertNotNull(dim2);
        assertTrue(dim2.isPercent());
        assertEquals(2.0f, dim2.getValue(), 0.001f);
    }

    @Test
    void testRoundTripVariousSyntax() {
        // Test that parse -> toString -> parse produces the same result
        String[] testCases = {
            "auto",
            "0",
            "100px",
            "50%",
            "12.5px",
            "33.33%"
        };

        for (String input : testCases) {
            TaffyDimension parsed1 = DimensionValue.parse(input);
            assertNotNull(parsed1, "Failed to parse: " + input);

            String serialized = DimensionValue.toString(parsed1);
            assertNotNull(serialized);

            TaffyDimension parsed2 = DimensionValue.parse(serialized);
            assertNotNull(parsed2);

            // Compare types
            assertEquals(parsed1.isAuto(), parsed2.isAuto(),
                "Auto mismatch for: " + input);
            assertEquals(parsed1.isLength(), parsed2.isLength(),
                "Length mismatch for: " + input);
            assertEquals(parsed1.isPercent(), parsed2.isPercent(),
                "Percent mismatch for: " + input);

            // Compare values for numeric types
            if (parsed1.isLength() || parsed1.isPercent()) {
                assertEquals(parsed1.getValue(), parsed2.getValue(), 0.001f,
                    "Value mismatch for: " + input);
            }
        }
    }

    @Test
    void testToStringNull() {
        // toString with null should return "auto"
        String result = DimensionValue.toString(null);
        assertEquals("auto", result);
    }

    @Test
    void testToStringAuto() {
        TaffyDimension dim = TaffyDimension.AUTO;
        String result = DimensionValue.toString(dim);
        assertEquals("auto", result);
    }

    @Test
    void testToStringLength() {
        TaffyDimension dim = TaffyDimension.length(100);
        String result = DimensionValue.toString(dim);
        assertEquals("100.0px", result);
    }

    @Test
    void testToStringPercent() {
        TaffyDimension dim = TaffyDimension.percent(0.5f);
        String result = DimensionValue.toString(dim);
        assertEquals("50.0%", result);
    }
}
