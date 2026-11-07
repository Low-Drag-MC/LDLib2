package com.lowdragmc.lowdraglib2.gui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFormattingUtilTest {

    @Test
    void keepsNumbersBelowThePrecisionAsTheyAre() {
        assertEquals("0", TextFormattingUtil.formatLongToCompactString(0, 3));
        assertEquals("999", TextFormattingUtil.formatLongToCompactString(999, 3));
    }

    @Test
    void showsOneDecimalWhileTheLeadingPartIsASingleDigit() {
        assertEquals("1.2k", TextFormattingUtil.formatLongToCompactString(1_234, 3));
        assertEquals("3.4M", TextFormattingUtil.formatLongToCompactString(3_456_789, 3));
        assertEquals("2.1G", TextFormattingUtil.formatLongToCompactString(Integer.MAX_VALUE, 3));
    }

    @Test
    void truncatesInsteadOfRounding() {
        assertEquals("1.9k", TextFormattingUtil.formatLongToCompactString(1_999, 3));
    }

    @Test
    void dropsAZeroDecimal() {
        assertEquals("1k", TextFormattingUtil.formatLongToCompactString(1_000, 3));
        assertEquals("1k", TextFormattingUtil.formatLongToCompactString(1_099, 3));
        assertEquals("5M", TextFormattingUtil.formatLongToCompactString(5_000_000, 3));
    }

    @Test
    void showsNoDecimalOnceTheLeadingPartHasTwoDigits() {
        assertEquals("12k", TextFormattingUtil.formatLongToCompactString(12_345, 3));
        assertEquals("123k", TextFormattingUtil.formatLongToCompactString(123_456, 3));
    }

    @Test
    void formatsBucketsTheSameWay() {
        assertEquals("1.5", TextFormattingUtil.formatLongToCompactStringBuckets(1_500, 3));
        assertEquals("1", TextFormattingUtil.formatLongToCompactStringBuckets(1_000, 3));
        assertEquals("2.5k", TextFormattingUtil.formatLongToCompactStringBuckets(2_500_000, 3));
    }
}
