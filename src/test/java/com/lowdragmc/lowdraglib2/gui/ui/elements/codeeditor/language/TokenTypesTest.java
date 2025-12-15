package com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TokenTypes
 */
class TokenTypesTest {

    @Test
    void testKeyword() {
        // Test dynamic keyword token type
        List<String> keywords = Arrays.asList("if", "else", "while", "for");
        TokenType keywordToken = TokenTypes.KEYWORD.createTokenType(keywords);
        
        assertTrue(matchesPattern(keywordToken.getPattern(), "if"));
        assertTrue(matchesPattern(keywordToken.getPattern(), "else"));
        assertTrue(matchesPattern(keywordToken.getPattern(), "while"));
        assertTrue(matchesPattern(keywordToken.getPattern(), "for"));
        
        // Should not match partial words
        assertFalse(matchesWhole(keywordToken.getPattern(), "iffy"));
        assertFalse(matchesWhole(keywordToken.getPattern(), "whileLoop"));
    }

    @Test
    void testIdentifier() {
        String pattern = TokenTypes.IDENTIFIER.getPattern();
        
        assertTrue(matchesPattern(pattern, "variable"));
        assertTrue(matchesPattern(pattern, "_private"));
        assertTrue(matchesPattern(pattern, "myVar123"));
        assertTrue(matchesPattern(pattern, "CamelCase"));
        
        // Should not start with number
        assertFalse(matchesWhole(pattern, "123invalid"));
    }

    @Test
    void testString() {
        String pattern = TokenTypes.STRING.getPattern();
        
        assertTrue(matchesPattern(pattern, "\"hello world\""));
        assertTrue(matchesPattern(pattern, "\"\""));
        assertTrue(matchesPattern(pattern, "\"escaped \\\" quote\""));
        assertTrue(matchesPattern(pattern, "\"line\\nbreak\""));
        
        // Unclosed string should not match fully
        assertFalse(matchesWhole(pattern, "\"unclosed"));
    }

    @Test
    void testComment() {
        String pattern = TokenTypes.COMMENT.getPattern();
        
        // Single line comment
        assertTrue(matchesPattern(pattern, "// this is a comment"));
        assertTrue(matchesPattern(pattern, "//comment"));
        
        // Multi-line comment
        assertTrue(matchesPattern(pattern, "/* comment */"));
        assertTrue(matchesPattern(pattern, "/* multi\nline\ncomment */"));
        assertTrue(matchesPattern(pattern, "/**/"));
    }

    @Test
    void testNumber() {
        String pattern = TokenTypes.NUMBER.getPattern();
        
        assertTrue(matchesPattern(pattern, "0"));
        assertTrue(matchesPattern(pattern, "123"));
        assertTrue(matchesPattern(pattern, "999"));
        
        // Should match numbers as whole words
        assertFalse(matchesWhole(pattern, "123abc"));
    }

    @Test
    void testOperator() {
        String pattern = TokenTypes.OPERATOR.getPattern();
        
        assertTrue(matchesPattern(pattern, "+"));
        assertTrue(matchesPattern(pattern, "-"));
        assertTrue(matchesPattern(pattern, "*"));
        assertTrue(matchesPattern(pattern, "/"));
        assertTrue(matchesPattern(pattern, "=="));
        assertTrue(matchesPattern(pattern, "!="));
        assertTrue(matchesPattern(pattern, "<="));
        assertTrue(matchesPattern(pattern, ">="));
        assertTrue(matchesPattern(pattern, "&&"));
        assertTrue(matchesPattern(pattern, "||"));
    }

    @Test
    void testWhitespace() {
        String pattern = TokenTypes.WHITESPACE.getPattern();
        
        assertTrue(matchesPattern(pattern, " "));
        assertTrue(matchesPattern(pattern, "   "));
        assertTrue(matchesPattern(pattern, "\t"));
        assertTrue(matchesPattern(pattern, "\n"));
        assertTrue(matchesPattern(pattern, " \t\n "));
    }

    @Test
    void testOther() {
        String pattern = TokenTypes.OTHER.getPattern();
        
        // Should match any single character
        assertTrue(matchesPattern(pattern, "@"));
        assertTrue(matchesPattern(pattern, "$"));
        assertTrue(matchesPattern(pattern, "%"));
        assertTrue(matchesPattern(pattern, "a"));
    }

    @Test
    void testCssClassSelector() {
        String pattern = TokenTypes.CSS_CLASS_SELECTOR.getPattern();
        
        assertTrue(matchesPattern(pattern, ".class-name"));
        assertTrue(matchesPattern(pattern, ".myClass"));
        assertTrue(matchesPattern(pattern, ".btn-primary"));
        assertTrue(matchesPattern(pattern, "._hidden"));
        
        assertFalse(matchesWhole(pattern, ".123invalid"));
    }

    @Test
    void testCssIdSelector() {
        String pattern = TokenTypes.CSS_ID_SELECTOR.getPattern();
        
        assertTrue(matchesPattern(pattern, "#main"));
        assertTrue(matchesPattern(pattern, "#header-section"));
        assertTrue(matchesPattern(pattern, "#_private"));
        
        // Should not match hex colors
        assertFalse(matchesWhole(pattern, "#123"));
        assertFalse(matchesWhole(pattern, "#ffffff"));
    }

    @Test
    void testCssCombinator() {
        String pattern = TokenTypes.CSS_COMBINATOR.getPattern();
        
        assertTrue(matchesPattern(pattern, ">"));
        assertTrue(matchesPattern(pattern, "+"));
        assertTrue(matchesPattern(pattern, "~"));
    }

    @Test
    void testCssPseudo() {
        String pattern = TokenTypes.CSS_PSEUDO.getPattern();
        
        assertTrue(matchesPattern(pattern, ":hover"));
        assertTrue(matchesPattern(pattern, ":active"));
        assertTrue(matchesPattern(pattern, "::before"));
        assertTrue(matchesPattern(pattern, "::after"));
        assertTrue(matchesPattern(pattern, ":nth-child(2)"));
        assertTrue(matchesPattern(pattern, ":not(.class)"));
    }

    @Test
    void testCssAttribute() {
        String pattern = TokenTypes.CSS_ATTRIBUTE.getPattern();
        
        assertTrue(matchesPattern(pattern, "[type='text']"));
        assertTrue(matchesPattern(pattern, "[href]"));
        assertTrue(matchesPattern(pattern, "[data-value='123']"));
        assertTrue(matchesPattern(pattern, "[]"));
    }

    @Test
    void testCssProperty() {
        String pattern = TokenTypes.CSS_PROPERTY.getPattern();
        
        assertTrue(matchesPattern(pattern, "color:"));
        assertTrue(matchesPattern(pattern, "background-color:"));
        assertTrue(matchesPattern(pattern, "margin:"));
        assertTrue(matchesPattern(pattern, "font-size:"));
    }

    @Test
    void testCssUnit() {
        String pattern = TokenTypes.CSS_UNIT.getPattern();
        
        assertTrue(matchesPattern(pattern, "10px"));
        assertTrue(matchesPattern(pattern, "1.5em"));
        assertTrue(matchesPattern(pattern, "100%"));
        assertTrue(matchesPattern(pattern, "50vh"));
        assertTrue(matchesPattern(pattern, "2rem"));
        assertTrue(matchesPattern(pattern, "90deg"));
        assertTrue(matchesPattern(pattern, "500ms"));
        assertTrue(matchesPattern(pattern, "0.5s"));
    }

    @Test
    void testCssColor() {
        String pattern = TokenTypes.CSS_COLOR.getPattern();
        
        // Hex colors
        assertTrue(matchesPattern(pattern, "#fff"));
        assertTrue(matchesPattern(pattern, "#ffffff"));
        assertTrue(matchesPattern(pattern, "#12345678"));
        assertTrue(matchesPattern(pattern, "#FFF"));
        
        // RGB/RGBA colors
        assertTrue(matchesPattern(pattern, "rgb(255, 0, 0)"));
        assertTrue(matchesPattern(pattern, "rgba(255, 0, 0, 0.5)"));
        assertTrue(matchesPattern(pattern, "rgb(100%, 50%, 0%)"));
        assertTrue(matchesPattern(pattern, "rgba(100%, 50%, 0%, 0.8)"));
    }

    @Test
    void testCssImportant() {
        String pattern = TokenTypes.CSS_IMPORTANT.getPattern();
        
        assertTrue(matchesPattern(pattern, "!important"));
    }

    @Test
    void testTokenTypeEquality() {
        TokenType token1 = new TokenType("TEST");
        TokenType token2 = new TokenType("TEST");
        TokenType token3 = new TokenType("OTHER");
        
        assertEquals(token1, token2);
        assertNotEquals(token1, token3);
        assertEquals(token1.hashCode(), token2.hashCode());
    }

    // Helper methods
    private boolean matchesPattern(String pattern, String input) {
        if (pattern == null) return false;
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(input);
        return m.find();
    }

    private boolean matchesWhole(String pattern, String input) {
        if (pattern == null) return false;
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(input);
        return m.matches();
    }
}
