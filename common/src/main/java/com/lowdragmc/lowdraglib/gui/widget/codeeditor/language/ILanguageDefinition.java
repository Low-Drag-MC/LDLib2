package com.lowdragmc.lowdraglib.gui.widget.codeeditor.language;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ILanguageDefinition {
    /**
     * Returns the name of the language
     */
    String getName();
    /**
     * Returns the pattern that matches the token
     */
    Pattern getTokenPattern();

    /**
     * Returns the token type for the given matcher
     */
    TokenType getTokenType(Matcher matcher);

    boolean shouldIncreaseIndentation(String trimmedLine);
}
