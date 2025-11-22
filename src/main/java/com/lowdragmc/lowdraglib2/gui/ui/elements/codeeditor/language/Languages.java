package com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language;

import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Set;

@UtilityClass
public final class Languages {
    public LanguageDefinition JAVASCRIPT = new LanguageDefinition("JavaScript", List.of(
            TokenTypes.KEYWORD.createTokenType(List.of("break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for", "function", "if", "import", "in", "instanceof", "let", "new", "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var", "void", "while", "with", "yield")),
            TokenTypes.IDENTIFIER,
            TokenTypes.STRING,
            TokenTypes.COMMENT,
            TokenTypes.NUMBER,
            TokenTypes.OPERATOR,
            TokenTypes.WHITESPACE,
            TokenTypes.OTHER), Set.of("{"));

    public LanguageDefinition LSS = new LanguageDefinition("LSS", List.of(
            // LSS Selector
            TokenTypes.CSS_CLASS_SELECTOR,
            TokenTypes.CSS_ID_SELECTOR,
            TokenTypes.CSS_COMBINATOR,
            TokenTypes.CSS_PSEUDO,
            TokenTypes.CSS_ATTRIBUTE,
            // LSS property
            TokenTypes.CSS_PROPERTY,
            TokenTypes.STRING,
            TokenTypes.COMMENT,
            TokenTypes.NUMBER,
            // LSS unit
            TokenTypes.CSS_UNIT,
            // LSS Color
            TokenTypes.CSS_COLOR,
            // LSS IMPORTANT
            TokenTypes.CSS_IMPORTANT,
            TokenTypes.IDENTIFIER,
            TokenTypes.WHITESPACE,
            TokenTypes.OTHER
    ), Set.of("{"));
}
