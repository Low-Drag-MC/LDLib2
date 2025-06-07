package com.lowdragmc.lowdraglib2.gui.widget.codeeditor.language;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class SyntaxParser {
    @Setter
    private ILanguageDefinition languageDefinition;

    public SyntaxParser() {
        this.languageDefinition = Languages.JAVASCRIPT;
    }

    public List<Token> parseLine(String lineText) {
        List<Token> tokens = new ArrayList<>();
        Pattern pattern = languageDefinition.getTokenPattern();
        Matcher matcher = pattern.matcher(lineText);

        while (matcher.find()) {
            String text = matcher.group();
            TokenType type = languageDefinition.getTokenType(matcher);
            tokens.add(new Token(text, type, matcher.start(), matcher.end()));
        }

        return tokens;
    }
}

