package com.lowdragmc.lowdraglib2.gui.widget.codeeditor.language;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.regex.Matcher;

@Accessors(chain = true)
public class TokenType implements Predicate<Matcher>{
    public final String name;
    @Getter
    @Setter
    private Predicate<Matcher> matcher;
    @Getter
    @Setter
    @Nullable
    private String pattern;

    public TokenType(String name) {
        this.name = name;
        this.matcher = m -> m.group(this.name) != null;
    }

    public boolean hasPattern() {
        return this.pattern != null;
    }

    @Override
    public boolean test(Matcher matcher) {
        return this.matcher.test(matcher);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TokenType) {
            return ((TokenType) obj).name.equals(name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}

