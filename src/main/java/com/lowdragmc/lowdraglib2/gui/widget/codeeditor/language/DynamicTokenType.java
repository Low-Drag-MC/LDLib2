package com.lowdragmc.lowdraglib2.gui.widget.codeeditor.language;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.function.Function;

@Accessors(chain = true)
public class DynamicTokenType<T> extends TokenType {
    @Getter
    private final Function<T, String> patternCreator;

    public DynamicTokenType(String name, Function<T, String> patternCreator) {
        super(name);
        this.patternCreator = patternCreator;
    }

    public TokenType createTokenType(T value) {
        return new TokenType(this.name).setPattern(this.patternCreator.apply(value));
    }

}

