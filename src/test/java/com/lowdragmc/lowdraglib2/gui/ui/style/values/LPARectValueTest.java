package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.LPARect;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LPARectValueTest {

    @Test
    void testParseSingleValue() {
        // All sides should be the same
        LPARect rect = LPARectValue.parse("10px");
        assertNotNull(rect);
        TaffyRect<LengthPercentageAuto> r = rect.rect();

        assertTrue(r.left.isLength());
        assertEquals(10f, r.left.getValue(), 0.001f);
        assertTrue(r.right.isLength());
        assertEquals(10f, r.right.getValue(), 0.001f);
        assertTrue(r.top.isLength());
        assertEquals(10f, r.top.getValue(), 0.001f);
        assertTrue(r.bottom.isLength());
        assertEquals(10f, r.bottom.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPARectValue.toString(rect);
        assertEquals("10.0px", serialized);
    }

    @Test
    void testParseTwoValues() {
        // Vertical, Horizontal
        LPARect rect = LPARectValue.parse("10px 20px");
        assertNotNull(rect);
        TaffyRect<LengthPercentageAuto> r = rect.rect();

        assertTrue(r.top.isLength());
        assertEquals(10f, r.top.getValue(), 0.001f);
        assertTrue(r.bottom.isLength());
        assertEquals(10f, r.bottom.getValue(), 0.001f);
        assertTrue(r.left.isLength());
        assertEquals(20f, r.left.getValue(), 0.001f);
        assertTrue(r.right.isLength());
        assertEquals(20f, r.right.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPARectValue.toString(rect);
        assertEquals("10.0px 20.0px", serialized);
    }

    @Test
    void testParseThreeValues() {
        // Top, Horizontal, Bottom
        LPARect rect = LPARectValue.parse("10px 20px 30px");
        assertNotNull(rect);
        TaffyRect<LengthPercentageAuto> r = rect.rect();

        assertTrue(r.top.isLength());
        assertEquals(10f, r.top.getValue(), 0.001f);
        assertTrue(r.left.isLength());
        assertEquals(20f, r.left.getValue(), 0.001f);
        assertTrue(r.right.isLength());
        assertEquals(20f, r.right.getValue(), 0.001f);
        assertTrue(r.bottom.isLength());
        assertEquals(30f, r.bottom.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPARectValue.toString(rect);
        assertEquals("10.0px 20.0px 30.0px", serialized);
    }

    @Test
    void testParseFourValues() {
        // Top, Right, Bottom, Left (clockwise from top)
        LPARect rect = LPARectValue.parse("10px 20px 30px 40px");
        assertNotNull(rect);
        TaffyRect<LengthPercentageAuto> r = rect.rect();

        assertTrue(r.top.isLength());
        assertEquals(10f, r.top.getValue(), 0.001f);
        assertTrue(r.right.isLength());
        assertEquals(20f, r.right.getValue(), 0.001f);
        assertTrue(r.bottom.isLength());
        assertEquals(30f, r.bottom.getValue(), 0.001f);
        assertTrue(r.left.isLength());
        assertEquals(40f, r.left.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPARectValue.toString(rect);
        assertEquals("10.0px 20.0px 30.0px 40.0px", serialized);
    }

    @Test
    void testParseMixedUnits() {
        // Mix of lengths, percentages, and keywords
        LPARect rect = LPARectValue.parse("10px 50% auto 20px");
        assertNotNull(rect);
        TaffyRect<LengthPercentageAuto> r = rect.rect();

        assertTrue(r.top.isLength());
        assertEquals(10f, r.top.getValue(), 0.001f);
        assertTrue(r.right.isPercent());
        assertEquals(0.5f, r.right.getValue(), 0.001f);
        assertTrue(r.bottom.isAuto());
        assertTrue(r.left.isLength());
        assertEquals(20f, r.left.getValue(), 0.001f);

        // Verify round-trip
        String serialized = LPARectValue.toString(rect);
        LPARect reparsed = LPARectValue.parse(serialized);
        assertNotNull(reparsed);
    }

    @Test
    void testParseAutoKeyword() {
        LPARect rect = LPARectValue.parse("auto");
        assertNotNull(rect);
        TaffyRect<LengthPercentageAuto> r = rect.rect();

        assertTrue(r.left.isAuto());
        assertTrue(r.right.isAuto());
        assertTrue(r.top.isAuto());
        assertTrue(r.bottom.isAuto());

        // Verify round-trip
        String serialized = LPARectValue.toString(rect);
        assertEquals("auto", serialized);
    }

    @Test
    void testParseIntrinsicSizing() {
        // Test with intrinsic sizing keywords
        LPARect rect = LPARectValue.parse("min-content max-content fit-content stretch");
        assertNotNull(rect);
        TaffyRect<LengthPercentageAuto> r = rect.rect();

        assertTrue(r.top.isMinContent());
        assertTrue(r.right.isMaxContent());
        assertTrue(r.bottom.isFitContent());
        assertTrue(r.left.isStretch());

        // Verify round-trip
        String serialized = LPARectValue.toString(rect);
        LPARect reparsed = LPARectValue.parse(serialized);
        assertNotNull(reparsed);
    }

    @Test
    void testParseEmpty() {
        // Empty string should return auto rect
        LPARect rect1 = LPARectValue.parse("");
        assertNotNull(rect1);
        assertTrue(rect1.rect().left.isAuto());

        // Null should return auto rect
        LPARect rect2 = LPARectValue.parse(null);
        assertNotNull(rect2);
        assertTrue(rect2.rect().left.isAuto());
    }

    @Test
    void testParseInvalid() {
        // Invalid syntax should return null
        LPARect rect = LPARectValue.parse("10px invalid 20px");
        assertNull(rect);
    }

    @Test
    void testParseWhitespace() {
        // Extra whitespace should be handled
        LPARect rect = LPARectValue.parse("  10px   20px  ");
        assertNotNull(rect);
        TaffyRect<LengthPercentageAuto> r = rect.rect();

        assertEquals(10f, r.top.getValue(), 0.001f);
        assertEquals(20f, r.left.getValue(), 0.001f);
    }

    @Test
    void testRoundTripShorthandOptimization() {
        // Test that round-trip uses optimal shorthand

        // All sides same -> single value
        LPARect rect1 = new LPARect(TaffyRect.all(LengthPercentageAuto.length(10)));
        String s1 = LPARectValue.toString(rect1);
        assertEquals("10.0px", s1);

        // Vertical/horizontal pairs -> two values
        LPARect rect2 = new LPARect(TaffyRect.hv(
            LengthPercentageAuto.length(20),
            LengthPercentageAuto.length(10)
        ));
        String s2 = LPARectValue.toString(rect2);
        assertEquals("10.0px 20.0px", s2);

        // Top, horizontal, bottom -> three values
        LPARect rect3 = new LPARect(new TaffyRect<>(
            LengthPercentageAuto.length(20),
            LengthPercentageAuto.length(20),
            LengthPercentageAuto.length(10),
            LengthPercentageAuto.length(30)
        ));
        String s3 = LPARectValue.toString(rect3);
        assertEquals("10.0px 20.0px 30.0px", s3);

        // All different -> four values
        LPARect rect4 = new LPARect(new TaffyRect<>(
            LengthPercentageAuto.length(40),
            LengthPercentageAuto.length(20),
            LengthPercentageAuto.length(10),
            LengthPercentageAuto.length(30)
        ));
        String s4 = LPARectValue.toString(rect4);
        assertEquals("10.0px 20.0px 30.0px 40.0px", s4);
    }

    @Test
    void testRoundTripVariousSyntax() {
        // Test that parse -> toString -> parse produces consistent results
        String[] testCases = {
            "auto",
            "10px",
            "10px 20px",
            "10px 20px 30px",
            "10px 20px 30px 40px",
            "50%",
            "min-content"
        };

        for (String input : testCases) {
            LPARect parsed1 = LPARectValue.parse(input);
            assertNotNull(parsed1, "Failed to parse: " + input);

            String serialized = LPARectValue.toString(parsed1);
            assertNotNull(serialized);

            LPARect parsed2 = LPARectValue.parse(serialized);
            assertNotNull(parsed2);

            // Compare all edges
            TaffyRect<LengthPercentageAuto> r1 = parsed1.rect();
            TaffyRect<LengthPercentageAuto> r2 = parsed2.rect();

            assertEquals(r1.left.getType(), r2.left.getType(), "Left type mismatch for: " + input);
            assertEquals(r1.right.getType(), r2.right.getType(), "Right type mismatch for: " + input);
            assertEquals(r1.top.getType(), r2.top.getType(), "Top type mismatch for: " + input);
            assertEquals(r1.bottom.getType(), r2.bottom.getType(), "Bottom type mismatch for: " + input);
        }
    }
}
