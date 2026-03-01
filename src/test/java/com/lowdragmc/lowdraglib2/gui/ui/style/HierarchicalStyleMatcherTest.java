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

    @Test
    void testParse_validSelector_notPseudoClass() {
        String selector = "div:not(.class1)";
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(selector);

        Assertions.assertNotNull(matcher, "Matcher should not be null for a valid :not selector.");
        Assertions.assertEquals("div:not(.class1)", matcher.toString(), "Matcher's toString should return the correct selector.");
    }

    @Test
    void testParse_validSelector_final() {
        String selector = "button:host :not(.label#id > child) > .class:host";
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(selector);

        Assertions.assertNotNull(matcher, "Matcher should not be null.");
        Assertions.assertEquals("button:host :not(.label#id > child) > .class:host", matcher.toString(), "Final Test error.");
    }

    // -----------------------------------------------------------------------
    // Generic pseudo-class sugar: :xxx  →  __xxx__  (and reverse in toString)
    // -----------------------------------------------------------------------

    @Test
    void testPseudo_singleState_hover() {
        // :hover is NOT a scope modifier → maps to __hover__ class
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse("button:hover");
        Assertions.assertNotNull(matcher);
        // toString should round-trip back to pseudo-class syntax
        Assertions.assertEquals("button:hover", matcher.toString());
        // Exactly one selector group containing the element + state class
        var groups = matcher.getSelectorGroups();
        Assertions.assertEquals(1, groups.size());
        var selectors = groups.get(0).styleMatcher().selector();
        Assertions.assertEquals(2, selectors.length, "Expected element + state-class selectors");
        Assertions.assertEquals(SelectorType.ELEMENT, selectors[0].type());
        Assertions.assertEquals(SelectorType.CLASS,   selectors[1].type());
        Assertions.assertEquals("__hovered__", selectors[1].identity().left().orElseThrow());
    }

    @Test
    void testPseudo_multipleStates() {
        // button:hover:focus → element + __hover__ + __focus__
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse("button:hover:focus");
        Assertions.assertNotNull(matcher);
        Assertions.assertEquals("button:hover:focus", matcher.toString());
        var selectors = matcher.getSelectorGroups().get(0).styleMatcher().selector();
        Assertions.assertEquals(3, selectors.length);
        Assertions.assertEquals("__hovered__", selectors[1].identity().left().orElseThrow());
        Assertions.assertEquals("__focus__", selectors[2].identity().left().orElseThrow());
    }

    @Test
    void testPseudo_customState() {
        // Any unknown pseudo-class should become __xxx__ — no source change required
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse("button:my-state");
        Assertions.assertNotNull(matcher);
        Assertions.assertEquals("button:my-state", matcher.toString());
        var selectors = matcher.getSelectorGroups().get(0).styleMatcher().selector();
        Assertions.assertEquals(2, selectors.length);
        Assertions.assertEquals("__my-state__", selectors[1].identity().left().orElseThrow());
    }

    @Test
    void testPseudo_scopeModifiersAreNotConvertedToClasses() {
        // :host and :internal are scope modifiers, NOT state classes
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse("button:host");
        Assertions.assertNotNull(matcher);
        Assertions.assertEquals("button:host", matcher.toString());
        var selectors = matcher.getSelectorGroups().get(0).styleMatcher().selector();
        // Should only be ONE selector (the element with HOST scope), not two
        Assertions.assertEquals(1, selectors.length);
        Assertions.assertEquals(SelectorType.ELEMENT, selectors[0].type());
        Assertions.assertEquals(SelectorScope.HOST, selectors[0].scope());
    }

    @Test
    void testPseudo_scopeAndStateCombo() {
        // button:hover:host → element(HOST) + __hover__ class
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse("button:hover:host");
        Assertions.assertNotNull(matcher);
        var selectors = matcher.getSelectorGroups().get(0).styleMatcher().selector();
        Assertions.assertEquals(2, selectors.length);
        // base selector carries HOST scope
        Assertions.assertEquals(SelectorScope.HOST, selectors[0].scope());
        // state class is scope-independent
        Assertions.assertEquals("__hovered__", selectors[1].identity().left().orElseThrow());
        Assertions.assertEquals(SelectorScope.ALL, selectors[1].scope());
    }

    @Test
    void testPseudo_classSelectorWithState() {
        // .panel:hover → CLASS("panel") + CLASS("__hover__")
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(".panel:hover");
        Assertions.assertNotNull(matcher);
        Assertions.assertEquals(".panel:hover", matcher.toString());
        var selectors = matcher.getSelectorGroups().get(0).styleMatcher().selector();
        Assertions.assertEquals(2, selectors.length);
        Assertions.assertEquals(SelectorType.CLASS, selectors[0].type());
        Assertions.assertEquals("panel", selectors[0].identity().left().orElseThrow());
        Assertions.assertEquals("__hovered__", selectors[1].identity().left().orElseThrow());
    }

    @Test
    void testPseudo_compoundSelectorWithChildCombinator() {
        // .panel:hover > button:disabled
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(".panel:hover > button:disabled");
        Assertions.assertNotNull(matcher);
        Assertions.assertEquals(".panel:hover > button:disabled", matcher.toString());
        var groups = matcher.getSelectorGroups();
        Assertions.assertEquals(2, groups.size());
        Assertions.assertFalse(groups.get(0).isChildCombinator(), "First group is not child combinator");
        Assertions.assertTrue(groups.get(1).isChildCombinator(),  "Second group is child combinator");
    }

    @Test
    void testPseudo_descendantSelectorWithState() {
        // div:hover span:focus
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse("div:hover span:focus");
        Assertions.assertNotNull(matcher);
        Assertions.assertEquals("div:hover span:focus", matcher.toString());
    }

    @Test
    void testPseudo_backwardCompat_explicitStateClass() {
        // Existing .__hovered__ class notation still parses correctly;
        // toString now prints it as :hovered (the canonical pseudo-class form).
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse("button.__hovered__");
        Assertions.assertNotNull(matcher);
        // "__hovered__" is printed as ":hovered"
        Assertions.assertEquals("button:hover", matcher.toString());
        var selectors = matcher.getSelectorGroups().get(0).styleMatcher().selector();
        Assertions.assertEquals(2, selectors.length);
        Assertions.assertEquals("__hovered__", selectors[1].identity().left().orElseThrow());
    }

    @Test
    void testPseudo_systemStateClasses_hovered_and_focused() {
        // The system adds __hovered__ and __focused__ to elements.
        // To target those, users should write :hovered and :focused (not :hover / :focus).
        HierarchicalStyleMatcher hoveredMatcher = HierarchicalStyleMatcher.parse("button:hovered");
        var hoveredSelectors = hoveredMatcher.getSelectorGroups().get(0).styleMatcher().selector();
        Assertions.assertEquals("__hovered__", hoveredSelectors[1].identity().left().orElseThrow(),
                "':hovered' should produce class '__hovered__' matching ModularUI.addClass(\"__hovered__\")");

        HierarchicalStyleMatcher focusedMatcher = HierarchicalStyleMatcher.parse("button:focused");
        var focusedSelectors = focusedMatcher.getSelectorGroups().get(0).styleMatcher().selector();
        Assertions.assertEquals("__focused__", focusedSelectors[1].identity().left().orElseThrow(),
                "':focused' should produce class '__focused__' matching ModularUI.addClass(\"__focused__\")");
    }

    @Test
    void testToString_stateClassRoundTrip() {
        // Verify that parsing and re-serialising a selector with multiple pseudo-classes
        // is idempotent (parse → toString → parse → toString produces the same string).
        String original = "div:hover:focus";
        String firstRound  = HierarchicalStyleMatcher.parse(original).toString();
        String secondRound = HierarchicalStyleMatcher.parse(firstRound).toString();
        Assertions.assertEquals(firstRound, secondRound, "toString should be idempotent");
        Assertions.assertEquals(original, firstRound, "First round-trip should reproduce original");
    }

    @Test
    void testToString_regularClassNotAffected() {
        // A class like "panel" (no __ wrapping) should still print as .panel
        HierarchicalStyleMatcher matcher = HierarchicalStyleMatcher.parse(".panel");
        Assertions.assertEquals(".panel", matcher.toString());
    }

}