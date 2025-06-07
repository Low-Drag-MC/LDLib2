package com.lowdragmc.lowdraglib2.gui.widget.codeeditor.language;

import java.util.List;
import java.util.Set;

public interface Languages {
    LanguageDefinition JAVASCRIPT = new LanguageDefinition("JavaScript", List.of(
            TokenTypes.KEYWORD.createTokenType(List.of("break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for", "function", "if", "import", "in", "instanceof", "let", "new", "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var", "void", "while", "with", "yield")),
            TokenTypes.IDENTIFIER,
            TokenTypes.STRING,
            TokenTypes.COMMENT,
            TokenTypes.NUMBER,
            TokenTypes.OPERATOR,
            TokenTypes.WHITESPACE,
            TokenTypes.OTHER), Set.of("{"));
}
