package com.lowdragmc.lowdraglib2.gui.ui.style;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HierarchicalStyleMatcherTest {

    /**
     * Tests the `parse` method of the `HierarchicalStyleMatcher` class.
     * Ensures that valid selector strings are correctly parsed into `HierarchicalStyleMatcher` instances,
     * and invalid strings throw the appropriate exceptions.
     */

    @Test
    void testParse_validSelector_singleElement() {
        String selector = "div";
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(selector);

        Assertions.assertNotNull(matcher, "Matcher should not be null for a valid single element selector.");
        Assertions.assertEquals("div", matcher.toString(), "Matcher's toString should return the correct selector.");
    }

    @Test
    void testParse_validSelector_elementWithClass() {
        String selector = "div.class1";
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(selector);

        Assertions.assertNotNull(matcher, "Matcher should not be null for a valid element with a class.");
        Assertions.assertEquals("div.class1", matcher.toString(), "Matcher's toString should return the correct selector.");
    }

    @Test
    void testParse_validSelector_elementWithId() {
        String selector = "div#myId";
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(selector);

        Assertions.assertNotNull(matcher, "Matcher should not be null for a valid element with an id.");
        Assertions.assertEquals("div#myId", matcher.toString(), "Matcher's toString should return the correct selector.");
    }

    @Test
    void testParse_validSelector_elementWithMultipleClasses() {
        String selector = "div.class1.class2";
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(selector);

        Assertions.assertNotNull(matcher, "Matcher should not be null for a valid element with multiple classes.");
        Assertions.assertEquals("div.class1.class2", matcher.toString(), "Matcher's toString should return the correct selector.");
    }

    @Test
    void testParse_validSelector_elementWithIdAndClass() {
        String selector = "div#myId.class1";
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(selector);

        Assertions.assertNotNull(matcher, "Matcher should not be null for a valid element with id and class.");
        Assertions.assertEquals("div#myId.class1", matcher.toString(), "Matcher's toString should return the correct selector.");
    }


    @Test
    void testParse_validSelector_childCombinator() {
        String selector = "div > span";
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(selector);

        Assertions.assertNotNull(matcher, "Matcher should not be null for a valid child combinator selector.");
        Assertions.assertEquals("div > span", matcher.toString(), "Matcher's toString should return the correct selector.");
    }

    @Test
    void testParse_validSelector_descendantCombinator() {
        String selector = "div span";
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(selector);

        Assertions.assertNotNull(matcher, "Matcher should not be null for a valid descendant combinator selector.");
        Assertions.assertEquals("div span", matcher.toString(), "Matcher's toString should return the correct selector.");
    }

    @Test
    void testParse_invalidSelector_emptyString() {
        String selector = "";

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> HierarchicalStyleMatcher.parse(selector),
                "Empty selector should throw IllegalArgumentException."
        );

        Assertions.assertEquals("Selector cannot be null or empty", exception.getMessage(), "Exception message mismatch.");
    }

    @Test
    void testParse_invalidSelector_endsWithCombinator() {
        String selector = "div > ";

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> HierarchicalStyleMatcher.parse(selector),
                "Selector ending with a combinator should throw IllegalArgumentException."
        );

        Assertions.assertEquals("Selector cannot end with a combinator: div > ", exception.getMessage(), "Exception message mismatch.");
    }

    @Test
    void testParse_invalidSelector_unexpectedCombinator() {
        String selector = "> span";

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> HierarchicalStyleMatcher.parse(selector),
                "Selector starting with a combinator should throw IllegalArgumentException."
        );

        Assertions.assertTrue(
                exception.getMessage().contains("Invalid selector near: >"),
                "Exception message should indicate the invalid combinator."
        );
    }
}