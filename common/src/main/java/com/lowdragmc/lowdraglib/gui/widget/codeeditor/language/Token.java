package com.lowdragmc.lowdraglib.gui.widget.codeeditor.language;

import lombok.Getter;

@Getter
public class Token {
    private String text;
    private TokenType type;
    private int startIndex;
    private int endIndex;

    public Token(String text, TokenType type, int startIndex, int endIndex) {
        this.text = text;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

}
