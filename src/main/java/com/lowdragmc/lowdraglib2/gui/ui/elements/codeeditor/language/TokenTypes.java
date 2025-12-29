package com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language;

import java.util.List;
import java.util.regex.Pattern;

public class TokenTypes {
    public static DynamicTokenType<List<String>> KEYWORD = new DynamicTokenType<>("Keyword", keywords -> {
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
    public static TokenType IDENTIFIER = new TokenType("Identifier").setPattern("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");
    public static TokenType STRING = new TokenType("String").setPattern("\"(\\\\.|[^\"])*\"");
    public static TokenType COMMENT = new TokenType("Comment").setPattern("//.*$|/\\*(.|\\R)*?\\*/");
    public static TokenType NUMBER = new TokenType("Number").setPattern("\\b\\d+\\b");
    public static TokenType OPERATOR = new TokenType("Operator").setPattern("[+\\-*/=<>!&|]+");
    public static TokenType WHITESPACE = new TokenType("Whitespace").setPattern("\\s+");
    public static TokenType OTHER = new TokenType("Other").setPattern(".");
    // LSS
    public static TokenType CSS_CLASS_SELECTOR = new TokenType("CSSClassSelector").setPattern("\\.[a-zA-Z_][a-zA-Z0-9_-]*");
    public static TokenType CSS_ID_SELECTOR = new TokenType("CSSIdSelector")
            .setPattern("(?<![a-zA-Z0-9_-])#(?![0-9a-fA-F]{3,4}\\b)(?![0-9a-fA-F]{6}\\b)(?![0-9a-fA-F]{8}\\b)[a-zA-Z_][a-zA-Z0-9_-]*\\b");
    public static TokenType CSS_COMBINATOR = new TokenType("CSSCombinator").setPattern("[>+~]");
    public static TokenType CSS_PSEUDO = new TokenType("CSSPseudo").setPattern(":+[a-zA-Z_-][a-zA-Z0-9_-]*(?:\\([^)]*\\))?");
    public static TokenType CSS_ATTRIBUTE = new TokenType("CSSAttribute").setPattern("\\[[^\\]]*\\]");
    public static TokenType CSS_PROPERTY = new TokenType("CSSProperty").setPattern("\\b[a-zA-Z-]+(?=\\s*:)");
    public static TokenType CSS_UNIT = new TokenType("CSSUnit").setPattern("\\b\\d+\\.?\\d*(px|em|rem|%|vh|vw|pt|cm|mm|in|pc|ex|ch|vmin|vmax|deg|rad|turn|s|ms)(?!\\w)");
    public static TokenType CSS_COLOR = new TokenType("CSSColor").setPattern("(?:#(?:[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\\b|rgba?\\(\\s*(?:\\d{1,3}%?\\s*,\\s*){2}\\d{1,3}%?(?:\\s*,\\s*(?:\\d*\\.\\d+|\\d+)%?)?\\s*\\))");
    public static TokenType CSS_IMPORTANT = new TokenType("CSSImportant").setPattern("!important");
    // XML
    public static TokenType XML_COMMENT = new TokenType("XMLComment").setPattern("<!--(.|\\R)*?-->");
    public static TokenType XML_CDATA = new TokenType("XMLCData").setPattern("<!\\[CDATA\\[(.|\\R)*?\\]\\]>");
    public static TokenType XML_ATTRIBUTE_VALUE = new TokenType("XMLAttributeValue").setPattern("\"[^\"]*\"|'[^']*'");
    public static TokenType XML_ATTRIBUTE_NAME = new TokenType("XMLAttributeName").setPattern("[a-zA-Z_:][a-zA-Z0-9_:.-]*(?=\\s*=)");
    public static TokenType XML_ENTITY_REF = new TokenType("XMLEntityRef").setPattern("&[a-zA-Z0-9#]+;");
    public static TokenType XML_TAG_NAME = new TokenType("XMLTagName").setPattern("</?[a-zA-Z_:][a-zA-Z0-9_:.-]*");
    public static TokenType XML_TAG_END = new TokenType("XMLTagEnd").setPattern("/?>");
    public static TokenType XML_EQ = new TokenType("XMLEqual").setPattern("=");
}
