package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LPAValueTest {

    @Test
    void testParseAuto() {
        LengthPercentageAuto lpa = LPAValue.parse("auto");
        assertNotNull(lpa);
        assertTrue(lpa.isAuto());

        // Verify round-trip
        String serialized = LPAValue.toString(lpa);
        assertEquals("auto", serialized);
    }

    @Test
    void testParseLength() {
        // With "px" unit
        LengthPercentageAuto lpa1 = LPAValue.parse("100px");
        assertNotNull(lpa1);
        assertTrue(lpa1.isLength());
        assertEquals(100f, lpa1.getValue(), 0.001f);

        // Without unit (assume px)
        LengthPercentageAuto lpa2 = LPAValue.parse("50");
        assertNotNull(lpa2);
        assertTrue(lpa2.isLength());
        assertEquals(50f, lpa2.getValue(), 0.001f);

        // Negative length
        LengthPercentageAuto lpa3 = LPAValue.parse("-25px");
        assertNotNull(lpa3);
        assertTrue(lpa3.isLength());
        assertEquals(-25f, lpa3.getValue(), 0.001f);

        // Verify round-trip
        assertEquals("100.0px", LPAValue.toString(lpa1));
        assertEquals("50.0px", LPAValue.toString(lpa2));
    }

    @Test
    void testParsePercentage() {
        LengthPercentageAuto lpa = LPAValue.parse("50%");
        assertNotNull(lpa);
        assertTrue(lpa.isPercent());
        // CSS percentages are 0-100, internally 0.0-1.0
        assertEquals(0.5f, lpa.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPAValue.toString(lpa);
        assertEquals("50.0%", serialized);
    }

    @Test
    void testParseMinContent() {
        LengthPercentageAuto lpa = LPAValue.parse("min-content");
        assertNotNull(lpa);
        assertTrue(lpa.isMinContent());

        // Verify round-trip
        String serialized = LPAValue.toString(lpa);
        assertEquals("min-content", serialized);
    }

    @Test
    void testParseMaxContent() {
        LengthPercentageAuto lpa = LPAValue.parse("max-content");
        assertNotNull(lpa);
        assertTrue(lpa.isMaxContent());

        // Verify round-trip
        String serialized = LPAValue.toString(lpa);
        assertEquals("max-content", serialized);
    }

    @Test
    void testParseFitContent() {
        LengthPercentageAuto lpa = LPAValue.parse("fit-content");
        assertNotNull(lpa);
        assertTrue(lpa.isFitContent());

        // Verify round-trip
        String serialized = LPAValue.toString(lpa);
        assertEquals("fit-content", serialized);
    }

    @Test
    void testParseStretch() {
        LengthPercentageAuto lpa = LPAValue.parse("stretch");
        assertNotNull(lpa);
        assertTrue(lpa.isStretch());

        // Verify round-trip
        String serialized = LPAValue.toString(lpa);
        assertEquals("stretch", serialized);
    }

    @Test
    void testParseCaseInsensitive() {
        LengthPercentageAuto lpa1 = LPAValue.parse("AUTO");
        assertTrue(lpa1.isAuto());

        LengthPercentageAuto lpa2 = LPAValue.parse("Min-Content");
        assertTrue(lpa2.isMinContent());

        LengthPercentageAuto lpa3 = LPAValue.parse("100PX");
        assertTrue(lpa3.isLength());
        assertEquals(100f, lpa3.getValue(), 0.001f);
    }

    @Test
    void testParseWhitespace() {
        // Extra whitespace should be handled
        LengthPercentageAuto lpa1 = LPAValue.parse("  100px  ");
        assertNotNull(lpa1);
        assertTrue(lpa1.isLength());
        assertEquals(100f, lpa1.getValue(), 0.001f);

        LengthPercentageAuto lpa2 = LPAValue.parse("  auto  ");
        assertNotNull(lpa2);
        assertTrue(lpa2.isAuto());
    }

    @Test
    void testParseEmpty() {
        // Empty string should return auto
        LengthPercentageAuto lpa1 = LPAValue.parse("");
        assertNotNull(lpa1);
        assertTrue(lpa1.isAuto());

        // Null should return auto
        LengthPercentageAuto lpa2 = LPAValue.parse(null);
        assertNotNull(lpa2);
        assertTrue(lpa2.isAuto());
    }

    @Test
    void testParseInvalid() {
        // Invalid syntax should return null
        LengthPercentageAuto lpa = LPAValue.parse("invalid");
        assertNull(lpa);
    }

    @Test
    void testParseFloatingPoint() {
        // Floating point lengths
        LengthPercentageAuto lpa1 = LPAValue.parse("12.5px");
        assertNotNull(lpa1);
        assertTrue(lpa1.isLength());
        assertEquals(12.5f, lpa1.getValue(), 0.001f);

        // Floating point percentages
        LengthPercentageAuto lpa2 = LPAValue.parse("33.33%");
        assertNotNull(lpa2);
        assertTrue(lpa2.isPercent());
        assertEquals(0.3333f, lpa2.getValue(), 0.001f);
    }

    @Test
    void testRoundTripVariousSyntax() {
        // Test that parse -> toString -> parse produces the same result
        String[] testCases = {
            "auto",
            "100px",
            "50%",
            "min-content",
            "max-content",
            "fit-content",
            "stretch"
        };

        for (String input : testCases) {
            LengthPercentageAuto parsed1 = LPAValue.parse(input);
            assertNotNull(parsed1, "Failed to parse: " + input);

            String serialized = LPAValue.toString(parsed1);
            assertNotNull(serialized);

            LengthPercentageAuto parsed2 = LPAValue.parse(serialized);
            assertNotNull(parsed2);

            // Compare types
            assertEquals(parsed1.getType(), parsed2.getType(),
                "Type mismatch for: " + input);

            // Compare values for numeric types
            if (parsed1.isLength() || parsed1.isPercent()) {
                assertEquals(parsed1.getValue(), parsed2.getValue(), 0.001f,
                    "Value mismatch for: " + input);
            }
        }
    }
}
