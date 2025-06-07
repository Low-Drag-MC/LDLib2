package com.lowdragmc.lowdraglib2.gui.widget.codeeditor.language;

import java.util.List;
import java.util.regex.Pattern;

public class TokenTypes {
    public static DynamicTokenType<List<String>> KEYWORD = new DynamicTokenType<>("KEYWORD", keywords -> {
        var patternBuilder = new StringBuilder();
        patternBuilder.append("\\b(");
        for (int i = 0; i < keywords.size(); i++) {
            patternBuilder.append(Pattern.quote(keywords.get(i)));
            if (i < keywords.size() - 1) {
                patternBuilder.append("|");
            }
        }
        patternBuilder.append(")\\b");
        return patternBuilder.toString();
    });
    public static TokenType IDENTIFIER = new TokenType("IDENTIFIER").setPattern("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");
    public static TokenType STRING = new TokenType("STRING").setPattern("\"(\\\\.|[^\"])*\"");
    public static TokenType COMMENT = new TokenType("COMMENT").setPattern("//.*$|/\\*(.|\\R)*?\\*/");
    public static TokenType NUMBER = new TokenType("NUMBER").setPattern("\\b\\d+\\b");
    public static TokenType OPERATOR = new TokenType("OPERATOR").setPattern("[+\\-*/=<>!&|]+");
    public static TokenType WHITESPACE = new TokenType("WHITESPACE").setPattern("\\s+");
    public static TokenType OTHER = new TokenType("OTHER").setPattern(".");
}
