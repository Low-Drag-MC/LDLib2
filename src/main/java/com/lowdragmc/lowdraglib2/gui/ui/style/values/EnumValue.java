package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.ValueParser;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

public class EnumValue<T extends Enum<T>> extends StyleValue<T> {
    private final Class<T> clazz;

    public EnumValue(Class<T> clazz, String rawValue) {
        super(rawValue);
        this.clazz = clazz;
    }

    public static <T extends Enum<T>> ValueParser<T> of(Class<T> clazz) {
        return raw -> new EnumValue<>(clazz, raw);
    }

    @Nullable
    public static <T extends Enum<T>> T find(Class<T> clazz, String rawValue) {
        var raw = rawValue.trim();
        var constants = clazz.getEnumConstants();
        for (var constant : constants) {
            if (constant.name().equalsIgnoreCase(raw)) {
                return constant;
            }
        }
        var normalized = normalize(raw);
        for (var constant : constants) {
            if (normalize(constant.name()).equals(normalized)) {
                return constant;
            }
        }
        return null;
    }

    public static <T extends Enum<T>> T parse(Class<T> clazz, String rawValue) {
        var value = find(clazz, rawValue);
        if (value == null) {
            throw new IllegalArgumentException("'" + rawValue + "' is not one of [" + Arrays.stream(clazz.getEnumConstants())
                    .map(constant -> constant.name().toLowerCase().replace('_', '-'))
                    .collect(Collectors.joining(", ")) + "]");
        }
        return value;
    }

    private static String normalize(String name) {
        var builder = new StringBuilder(name.length());
        for (var i = 0; i < name.length(); i++) {
            var c = name.charAt(i);
            if (c == '-' || c == '_' || Character.isWhitespace(c)) continue;
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    @Override
    protected T doCompute(String rawValue) {
        return parse(clazz, rawValue);
    }
}
