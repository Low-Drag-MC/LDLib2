package com.lowdragmc.lowdraglib2.uitest.record;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptionsTest {

    @Test
    void aCaptionLastsUntilTheNextInItsLanguage() {
        var captions = new Captions();
        captions.caption("en", "One", 0.5);
        captions.caption("zh", "一", 0.5);
        captions.caption("en", "Two", 2.25);
        var vtt = captions.toVtt(4);
        assertEquals("""
                WEBVTT

                00:00:00.500 --> 00:00:02.250
                One

                00:00:02.250 --> 00:00:04.000
                Two
                """, vtt.get("en"));
        assertEquals("""
                WEBVTT

                00:00:00.500 --> 00:00:04.000
                一
                """, vtt.get("zh"));
    }

    @Test
    void anEmptyCaptionClearsWithoutStartingAnother() {
        var captions = new Captions();
        captions.caption("en", "Shown", 1);
        captions.caption("en", "", 3);
        assertEquals("""
                WEBVTT

                00:00:01.000 --> 00:00:03.000
                Shown
                """, captions.toVtt(10).get("en"));
    }

    @Test
    void aCaptionReplacedInTheSameInstantIsDropped() {
        var captions = new Captions();
        captions.caption("en", "Gone at once", 1);
        captions.caption("en", "Kept", 1);
        var vtt = captions.toVtt(2).get("en");
        assertFalse(vtt.contains("Gone at once"));
        assertTrue(vtt.contains("Kept"));
    }

    @Test
    void aLanguageWithNothingEverShownWritesNoFile() {
        var captions = new Captions();
        captions.caption("en", "", 1);
        captions.caption("zh", "瞬间", 2);
        var vtt = captions.toVtt(2);
        assertFalse(vtt.containsKey("en"));
        assertFalse(vtt.containsKey("zh"));
    }

    @Test
    void aBlankLineCannotEndACueEarly() {
        var captions = new Captions();
        captions.caption("en", "first\n\nsecond", 0);
        assertTrue(captions.toVtt(1).get("en").contains("first\nsecond"));
    }

    @Test
    void markupAndArrowsAreEscaped() {
        assertEquals("a &lt;b&gt; &amp; c --&gt; d", Captions.escape("a <b> & c --> d"));
    }

    @Test
    void timestampsCarryHoursAndRoundToMilliseconds() {
        assertEquals("00:00:00.000", Captions.timestamp(0));
        assertEquals("00:01:01.235", Captions.timestamp(61.2346));
        assertEquals("01:00:00.000", Captions.timestamp(3600));
        assertEquals("00:00:00.000", Captions.timestamp(-0.4));
    }
}
