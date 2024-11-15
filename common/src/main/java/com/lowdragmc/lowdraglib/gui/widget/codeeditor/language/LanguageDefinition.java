package com.lowdragmc.lowdraglib.gui.widget.codeeditor.language;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class LanguageDefinition implements ILanguageDefinition {

    private final String name;
    private final List<TokenType> typesInOrder;
    private final Set<String> indentations;
    private Pattern tokenPattern;

    public LanguageDefinition(String name, List<TokenType> typesInOrder, Set<String> indentations) {
        this.name = name;
        this.typesInOrder = typesInOrder;
        this.indentations = indentations;
    }

    public LanguageDefinition compileTokenPattern() {
        var patternBuilder = new StringBuilder();
        for (TokenType tokenType : typesInOrder) {
            if (tokenType.hasPattern()) {
                patternBuilder.append("|(?<").append(tokenType.name).append(">").append(tokenType.getPattern()).append(")");
            }
        }
        if (!patternBuilder.isEmpty()) {
            patternBuilder.deleteCharAt(0);
        }
        tokenPattern = Pattern.compile(patternBuilder.toString());
        return this;
    }

    public Pattern getTokenPattern() {
        if (tokenPattern == null) {
            compileTokenPattern();
        }
        return tokenPattern;
    }

    @Nullable
    public TokenType getTokenType(Matcher matcher) {
        for (var type : typesInOrder) {
            if (type.test(matcher)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public boolean shouldIncreaseIndentation(String trimmedLine) {
        for (String indentation : indentations) {
            if (trimmedLine.endsWith(indentation)) {
                return true;
            }
        }
        return false;
    }
}

