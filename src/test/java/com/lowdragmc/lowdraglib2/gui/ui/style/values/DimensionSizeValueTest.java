package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.DimensionSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DimensionSizeValueTest {

    @Test
    void testParseSingleValue() {
        // Both dimensions should be the same
        DimensionSize size = DimensionSizeValue.parse("10px");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(10f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isLength());
        assertEquals(10f, s.height.getValue(), 0.001f);

        // Verify round-trip
        String serialized = DimensionSizeValue.toString(size);
        assertEquals("10.0px", serialized);
    }

    @Test
    void testParseTwoValues() {
        // Width, Height
        DimensionSize size = DimensionSizeValue.parse("10px 20px");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(10f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isLength());
        assertEquals(20f, s.height.getValue(), 0.001f);

        // Verify round-trip
        String serialized = DimensionSizeValue.toString(size);
        assertEquals("10.0px 20.0px", serialized);
    }

    @Test
    void testParseAuto() {
        DimensionSize size = DimensionSizeValue.parse("auto");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isAuto());
        assertTrue(s.height.isAuto());

        // Verify round-trip
        String serialized = DimensionSizeValue.toString(size);
        assertEquals("auto", serialized);
    }

    @Test
    void testParseMixedUnits() {
        // Mix of lengths, percentages, and auto
        DimensionSize size = DimensionSizeValue.parse("10px 50%");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(10f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(0.5f, s.height.getValue(), 0.001f);

        // Verify round-trip
        String serialized = DimensionSizeValue.toString(size);
        assertEquals("10.0px 50.0%", serialized);
    }

    @Test
    void testParseMixedWithAuto() {
        // Mix of auto with length
        DimensionSize size = DimensionSizeValue.parse("auto 100px");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isAuto());
        assertTrue(s.height.isLength());
        assertEquals(100f, s.height.getValue(), 0.001f);

        // Verify round-trip
        String serialized = DimensionSizeValue.toString(size);
        assertEquals("auto 100.0px", serialized);
    }

    @Test
    void testParsePercentages() {
        DimensionSize size = DimensionSizeValue.parse("50% 75%");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isPercent());
        assertEquals(0.5f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(0.75f, s.height.getValue(), 0.001f);

        // Verify round-trip
        String serialized = DimensionSizeValue.toString(size);
        assertEquals("50.0% 75.0%", serialized);
    }

    @Test
    void testParseFloatingPoint() {
        // Floating point values
        DimensionSize size = DimensionSizeValue.parse("12.5px 33.33%");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(12.5f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(0.3333f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseNegativeValues() {
        // Negative lengths
        DimensionSize size = DimensionSizeValue.parse("-10px -5px");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(-10f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isLength());
        assertEquals(-5f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseUnitless() {
        // Unitless values (assume pixels)
        DimensionSize size = DimensionSizeValue.parse("100 200");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(100f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isLength());
        assertEquals(200f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseCaseInsensitive() {
        DimensionSize size1 = DimensionSizeValue.parse("AUTO");
        assertTrue(size1.size().width.isAuto());

        DimensionSize size2 = DimensionSizeValue.parse("100PX 50%");
        assertNotNull(size2);
        TaffySize<TaffyDimension> s = size2.size();

        assertTrue(s.width.isLength());
        assertEquals(100f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(0.5f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseWhitespace() {
        // Extra whitespace should be handled
        DimensionSize size = DimensionSizeValue.parse("  10px   20px  ");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertEquals(10f, s.width.getValue(), 0.001f);
        assertEquals(20f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseMultipleSpaces() {
        // Multiple spaces between values
        DimensionSize size = DimensionSizeValue.parse("10px     20px");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertEquals(10f, s.width.getValue(), 0.001f);
        assertEquals(20f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseEmpty() {
        // Empty string should return AUTO (based on user's change)
        DimensionSize size1 = DimensionSizeValue.parse("");
        assertNotNull(size1);
        assertEquals(DimensionSize.AUTO, size1);

        // Null should return AUTO
        DimensionSize size2 = DimensionSizeValue.parse(null);
        assertNotNull(size2);
        assertEquals(DimensionSize.AUTO, size2);
    }

    @Test
    void testParseInvalid() {
        // Invalid syntax should return null
        DimensionSize size = DimensionSizeValue.parse("10px invalid");
        assertNull(size);
    }

    @Test
    void testParseTooManyValues() {
        // More than 2 values - should only use first two
        DimensionSize size = DimensionSizeValue.parse("10px 20px 30px");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        // Should use first two values
        assertEquals(10f, s.width.getValue(), 0.001f);
        assertEquals(20f, s.height.getValue(), 0.001f);
    }

    @Test
    void testToStringOptimization() {
        // When both dimensions are the same, should output single value
        DimensionSize size1 = new DimensionSize(TaffySize.all(TaffyDimension.length(10)));
        String s1 = DimensionSizeValue.toString(size1);
        assertEquals("10.0px", s1);

        // When both are auto
        DimensionSize size2 = new DimensionSize(TaffySize.all(TaffyDimension.AUTO));
        String s2 = DimensionSizeValue.toString(size2);
        assertEquals("auto", s2);

        // When dimensions differ, should output both
        DimensionSize size3 = new DimensionSize(new TaffySize<>(
            TaffyDimension.length(10),
            TaffyDimension.length(20)
        ));
        String s3 = DimensionSizeValue.toString(size3);
        assertEquals("10.0px 20.0px", s3);
    }

    @Test
    void testToStringNull() {
        // toString with null should return "auto"
        String result = DimensionSizeValue.toString(null);
        assertEquals("auto", result);
    }

    @Test
    void testToStringMixedUnits() {
        DimensionSize size = new DimensionSize(new TaffySize<>(
            TaffyDimension.length(10),
            TaffyDimension.percent(0.5f)
        ));
        String result = DimensionSizeValue.toString(size);
        assertEquals("10.0px 50.0%", result);
    }

    @Test
    void testToStringMixedWithAuto() {
        DimensionSize size = new DimensionSize(new TaffySize<>(
            TaffyDimension.AUTO,
            TaffyDimension.length(100)
        ));
        String result = DimensionSizeValue.toString(size);
        assertEquals("auto 100.0px", result);
    }

    @Test
    void testRoundTripVariousSyntax() {
        // Test that parse -> toString -> parse produces consistent results
        String[] testCases = {
            "auto",
            "10px",
            "10px 20px",
            "50%",
            "50% 75%",
            "10px 50%",
            "auto 100px",
            "100px auto",
            "12.5px 33.33%"
        };

        for (String input : testCases) {
            DimensionSize parsed1 = DimensionSizeValue.parse(input);
            assertNotNull(parsed1, "Failed to parse: " + input);

            String serialized = DimensionSizeValue.toString(parsed1);
            assertNotNull(serialized);

            DimensionSize parsed2 = DimensionSizeValue.parse(serialized);
            assertNotNull(parsed2);

            // Compare dimensions
            TaffySize<TaffyDimension> s1 = parsed1.size();
            TaffySize<TaffyDimension> s2 = parsed2.size();

            assertEquals(s1.width.isAuto(), s2.width.isAuto(),
                "Width auto mismatch for: " + input);
            assertEquals(s1.width.isLength(), s2.width.isLength(),
                "Width length mismatch for: " + input);
            assertEquals(s1.width.isPercent(), s2.width.isPercent(),
                "Width percent mismatch for: " + input);

            assertEquals(s1.height.isAuto(), s2.height.isAuto(),
                "Height auto mismatch for: " + input);
            assertEquals(s1.height.isLength(), s2.height.isLength(),
                "Height length mismatch for: " + input);
            assertEquals(s1.height.isPercent(), s2.height.isPercent(),
                "Height percent mismatch for: " + input);

            // Compare values for numeric types
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
        DimensionSize size = DimensionSizeValue.parse("100% 100%");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isPercent());
        assertEquals(1.0f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(1.0f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseSmallValues() {
        DimensionSize size = DimensionSizeValue.parse("0.5px 0.1%");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(0.5f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(0.001f, s.height.getValue(), 0.0001f);
    }

    @Test
    void testParseLargeValues() {
        DimensionSize size = DimensionSizeValue.parse("9999px 200%");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(9999f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isPercent());
        assertEquals(2.0f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseZero() {
        DimensionSize size = DimensionSizeValue.parse("0");
        assertNotNull(size);
        TaffySize<TaffyDimension> s = size.size();

        assertTrue(s.width.isLength());
        assertEquals(0f, s.width.getValue(), 0.001f);
        assertTrue(s.height.isLength());
        assertEquals(0f, s.height.getValue(), 0.001f);
    }

    @Test
    void testParseAllAutoVariations() {
        // Test various auto combinations
        String[] autoTests = {
            "auto",
            "auto auto",
            "AUTO",
            "  auto  "
        };

        for (String input : autoTests) {
            DimensionSize size = DimensionSizeValue.parse(input);
            assertNotNull(size, "Failed to parse: " + input);
            assertTrue(size.size().width.isAuto(), "Width not auto for: " + input);
            assertTrue(size.size().height.isAuto(), "Height not auto for: " + input);
        }
    }
}
