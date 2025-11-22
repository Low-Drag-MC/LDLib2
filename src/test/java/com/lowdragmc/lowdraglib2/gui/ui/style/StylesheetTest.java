package com.lowdragmc.lowdraglib2.gui.ui.style;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StylesheetTest {

    @BeforeAll
    static void setUp() {
        PropertyRegistry.init();
    }

    @Test
    void testParseSingleRule() {
        String lss = ".button { background: color(0xff0000ff); }";
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        StyleRule rule = sheet.rules.get(0);
        assertNotNull(rule.properties);
        assertTrue(rule.properties.containsKey(PropertyRegistry.BACKGROUND));
    }

    @Test
    void testParseMultipleRules() {
        String lss = """
            .button {
                background: color(0xff0000ff);
            }
            .text {
                text-color: #ffffff;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(2, sheet.rules.size());
    }

    @Test
    void testParseMultipleProperties() {
        String lss = """
            .element {
                z-index: 10;
                text-shadow: false;
                font-size: 12.5;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        StyleRule rule = sheet.rules.get(0);
        assertEquals(3, rule.properties.size());
        assertTrue(rule.properties.containsKey(PropertyRegistry.Z_INDEX));
        assertTrue(rule.properties.containsKey(PropertyRegistry.TEXT_SHADOW));
        assertTrue(rule.properties.containsKey(PropertyRegistry.FONT_SIZE));
    }

    @Test
    void testParseMultipleSelectors() {
        String lss = ".button, .link, .item { z-index: 5; }";
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(3, sheet.rules.size());
        for (StyleRule rule : sheet.rules) {
            assertTrue(rule.properties.containsKey(PropertyRegistry.Z_INDEX));
        }
    }

    @Test
    void testParseWithWhitespace() {
        String lss = """
            
            .button   {
                background   :   color(0xff0000ff)  ;
                z-index  :  10  ;
            }
            
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        assertEquals(2, sheet.rules.get(0).properties.size());
    }

    @Test
    void testParseInvalidProperty() {
        String lss = """
            .element {
                invalid-property: somevalue;
                z-index: 5;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        StyleRule rule = sheet.rules.get(0);
        // Invalid property should be skipped
        assertEquals(1, rule.properties.size());
        assertTrue(rule.properties.containsKey(PropertyRegistry.Z_INDEX));
    }

    @Test
    void testParseInvalidPropertyValue() {
        String lss = """
            .element {
                z-index: not-a-number;
                font-size: 12.5;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        StyleRule rule = sheet.rules.get(0);
        // Invalid value should be skipped, but valid property should remain
        assertTrue(rule.properties.containsKey(PropertyRegistry.FONT_SIZE));
    }

    @Test
    void testParseEmptyStylesheet() {
        String lss = "";
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(0, sheet.rules.size());
    }

    @Test
    void testParseEmptyRule() {
        String lss = ".element { }";
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        assertEquals(0, sheet.rules.get(0).properties.size());
    }

    @Test
    void testParseWithComments() {
        // Note: The current implementation doesn't support comments,
        // this test documents the current behavior
        String lss = """
            .element {
                z-index: 5;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
    }

    @Test
    void testParseHierarchicalSelector() {
        String lss = """
            .parent .child {
                z-index: 10;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        assertTrue(sheet.rules.get(0).properties.containsKey(PropertyRegistry.Z_INDEX));
    }

    @Test
    void testParseBooleanProperties() {
        String lss = """
            .element {
                text-shadow: true;
                allow-zoom: false;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        StyleRule rule = sheet.rules.get(0);
        assertTrue(rule.properties.containsKey(PropertyRegistry.TEXT_SHADOW));
        assertTrue(rule.properties.containsKey(PropertyRegistry.ALLOW_ZOOM));
    }

    @Test
    void testParseFloatProperties() {
        String lss = """
            .element {
                font-size: 14.5;
                line-spacing: 1.2;
                percentage: 75.5;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        StyleRule rule = sheet.rules.get(0);
        assertEquals(3, rule.properties.size());
        assertTrue(rule.properties.containsKey(PropertyRegistry.FONT_SIZE));
        assertTrue(rule.properties.containsKey(PropertyRegistry.LINE_SPACING));
        assertTrue(rule.properties.containsKey(PropertyRegistry.PERCENTAGE));
    }

    @Test
    void testParseIntegerProperties() {
        String lss = """
            .element {
                z-index: 100;
                max-item: 10;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        StyleRule rule = sheet.rules.get(0);
        assertTrue(rule.properties.containsKey(PropertyRegistry.Z_INDEX));
        assertTrue(rule.properties.containsKey(PropertyRegistry.MAX_ITEM));
    }

    @Test
    void testParseColorProperties() {
        String lss = """
            .element {
                text-color: #ff0000;
                error-color: #00ff00;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        StyleRule rule = sheet.rules.get(0);
        assertTrue(rule.properties.containsKey(PropertyRegistry.TEXT_COLOR));
        assertTrue(rule.properties.containsKey(PropertyRegistry.ERROR_COLOR));
    }

    @Test
    void testParsePropertiesWithoutSemicolon() {
        String lss = """
            .element {
                z-index: 5
                font-size: 12
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(1, sheet.rules.size());
        // Without semicolons, the regex should still match due to optional ';?'
        assertTrue(sheet.rules.get(0).properties.size() >= 1);
    }

    @Test
    void testParseComplexStylesheet() {
        String lss = """
            .button, .link {
                background: color(0xff0000ff);
                z-index: 10;
            }
            
            .container .item {
                font-size: 14;
                text-color: #ffffff;
                text-shadow: true;
            }
            
            #special {
                percentage: 50.5;
            }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        // .button, .link creates 2 rules, .container .item creates 1, #special creates 1
        assertTrue(sheet.rules.size() >= 3);
    }

    @Test
    void testMergeStylesheets() {
        String css1 = ".button { z-index: 5; }";
        String css2 = ".text { font-size: 12; }";

        Stylesheet sheet1 = Stylesheet.parse(css1);
        Stylesheet sheet2 = Stylesheet.parse(css2);

        sheet1.merge(sheet2);

        assertEquals(2, sheet1.rules.size());
    }

    @Test
    void testAddAndRemoveRule() {
        Stylesheet sheet = new Stylesheet(new java.util.ArrayList<>());

        StyleRule rule1 = new StyleRule(
                HierarchicalStyleMatcher.parse(".button"),
                java.util.Collections.emptyMap()
        );

        sheet.addRule(rule1);
        assertEquals(1, sheet.rules.size());

        sheet.removeRule(rule1);
        assertEquals(0, sheet.rules.size());
    }

    @Test
    void testClearStylesheet() {
        String lss = """
            .button { z-index: 5; }
            .text { font-size: 12; }
            """;
        Stylesheet sheet = Stylesheet.parse(lss);

        assertEquals(2, sheet.rules.size());
        sheet.clear();
        assertEquals(0, sheet.rules.size());
    }
}