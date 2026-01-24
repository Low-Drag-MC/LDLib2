package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.LPSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LPSizeValueTest {

    @Test
    void testParseSingleValue() {
        // Both dimensions should be the same
        LPSize size = LPSizeValue.parse("10px");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(10f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isLength());
        assertEquals(10f, s.height.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPSizeValue.toString(size);
        assertEquals("10.0px", serialized);
    }

    @Test
    void testParseTwoValues() {
        // Width, Height
        LPSize size = LPSizeValue.parse("10px 20px");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(10f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isLength());
        assertEquals(20f, s.height.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPSizeValue.toString(size);
        assertEquals("10.0px 20.0px", serialized);
    }

    @Test
    void testParseMixedUnits() {
        // Mix of lengths and percentages
        LPSize size = LPSizeValue.parse("10px 50%");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(10f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(0.5f, s.height.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPSizeValue.toString(size);
        assertEquals("10.0px 50.0%", serialized);
    }

    @Test
    void testParsePercentages() {
        LPSize size = LPSizeValue.parse("50% 75%");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isPercent());
        assertEquals(0.5f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(0.75f, s.height.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPSizeValue.toString(size);
        assertEquals("50.0% 75.0%", serialized);
    }

    @Test
    void testParseZero() {
        LPSize size = LPSizeValue.parse("0");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(0f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isLength());
        assertEquals(0f, s.height.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPSizeValue.toString(size);
        assertEquals("0.0px", serialized);
    }

    @Test
    void testParseFloatingPoint() {
        // Floating point values
        LPSize size = LPSizeValue.parse("12.5px 33.33%");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(12.5f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(0.3333f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseNegativeValues() {
        // Negative lengths
        LPSize size = LPSizeValue.parse("-10px -5px");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(-10f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isLength());
        assertEquals(-5f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseUnitless() {
        // Unitless values (assume pixels)
        LPSize size = LPSizeValue.parse("100 200");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(100f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isLength());
        assertEquals(200f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseCaseInsensitive() {
        LPSize size = LPSizeValue.parse("100PX 50%");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(100f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(0.5f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseWhitespace() {
        // Extra whitespace should be handled
        LPSize size = LPSizeValue.parse("  10px   20px  ");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertEquals(10f, s.width.getValue(), 0.001f);
        assertEquals(20f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseMultipleSpaces() {
        // Multiple spaces between values
        LPSize size = LPSizeValue.parse("10px     20px");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertEquals(10f, s.width.getValue(), 0.001f);
        assertEquals(20f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseEmpty() {
        // Empty string should return ZERO
        LPSize size1 = LPSizeValue.parse("");
        assertNotNull(size1);
        assertEquals(LPSize.ZERO, size1);

        // Null should return ZERO
        LPSize size2 = LPSizeValue.parse(null);
        assertNotNull(size2);
        assertEquals(LPSize.ZERO, size2);
    }

    @Test
    void testParseInvalid() {
        // Invalid syntax should return null
        LPSize size = LPSizeValue.parse("10px invalid");
        assertNull(size);
    }

    @Test
    void testParseTooManyValues() {
        // More than 2 values - should only use first two
        LPSize size = LPSizeValue.parse("10px 20px 30px");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        // Should use first two values
        assertEquals(10f, s.width.getValue(), 0.001f);
        assertEquals(20f, s.height.getValue(), 0.001f);
    }

    @Test
    void testToStringOptimization() {
        // When both dimensions are the same, should output single value
        LPSize size1 = new LPSize(TaffySize.all(LengthPercentage.length(10)));
        String s1 = LPSizeValue.toString(size1);
        assertEquals("10.0px", s1);

        // When dimensions differ, should output both
        LPSize size2 = new LPSize(new TaffySize<>(
            LengthPercentage.length(10),
            LengthPercentage.length(20)
        ));
        String s2 = LPSizeValue.toString(size2);
        assertEquals("10.0px 20.0px", s2);
    }

    @Test
    void testToStringNull() {
        // toString with null should return "0"
        String result = LPSizeValue.toString(null);
        assertEquals("0", result);
    }

    @Test
    void testToStringMixedUnits() {
        LPSize size = new LPSize(new TaffySize<>(
            LengthPercentage.length(10),
            LengthPercentage.percent(0.5f)
        ));
        String result = LPSizeValue.toString(size);
        assertEquals("10.0px 50.0%", result);
    }

    @Test
    void testRoundTripVariousSyntax() {
        // Test that parse -> toString -> parse produces consistent results
        String[] testCases = {
            "0",
            "10px",
            "10px 20px",
            "50%",
            "50% 75%",
            "10px 50%",
            "12.5px 33.33%"
        };

        for (String input : testCases) {
            LPSize parsed1 = LPSizeValue.parse(input);
            assertNotNull(parsed1, "Failed to parse: " + input);

            String serialized = LPSizeValue.toString(parsed1);
            assertNotNull(serialized);

            LPSize parsed2 = LPSizeValue.parse(serialized);
            assertNotNull(parsed2);

            // Compare dimensions
            TaffySize<LengthPercentage> s1 = parsed1.size();
            TaffySize<LengthPercentage> s2 = parsed2.size();

            assertEquals(s1.width.isLength(), s2.width.isLength(),
                "Width type mismatch for: " + input);
            assertEquals(s1.height.isLength(), s2.height.isLength(),
                "Height type mismatch for: " + input);

            // Compare values
            if (s1.width.isLength() || s1.width.isPercent()) {
                assertEquals(s1.width.getValue(), s2.width.getValue(), 0.001f,
                    "Width value mismatch for: " + input);
            }
            if (s1.height.isLength() || s1.height.isPercent()) {
                assertEquals(s1.height.getValue(), s2.height.getValue(), 0.001f,
                    "Height value mismatch for: " + input);
            }
        }
    }

    @Test
    void testParseHundredPercent() {
        LPSize size = LPSizeValue.parse("100% 100%");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isPercent());
        assertEquals(1.0f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(1.0f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseSmallValues() {
        LPSize size = LPSizeValue.parse("0.5px 0.1%");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(0.5f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(0.001f, s.height.getValue(), 0.0001f);
    }

    @Test
    void testParseLargeValues() {
        LPSize size = LPSizeValue.parse("9999px 200%");
        assertNotNull(size);
        TaffySize<LengthPercentage> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(9999f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(2.0f, s.height.getValue(), 0.001f);
    }
}
