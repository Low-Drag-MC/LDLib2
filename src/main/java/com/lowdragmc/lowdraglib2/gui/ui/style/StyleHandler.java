package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface StyleHandler<VALUE> {
    record Simple<T>(String name, Function<String, StyleValue<T>> creator) implements StyleHandler<T> {
        public static <T> Simple<T> of(String name, Function<String, StyleValue<T>> creator) {
            return new Simple<>(name, creator);
        }

        @Override
        public String getStyleName() {
            return name;
        }

        @Override
        public StyleValue<T> createStyleValue(String rawValue) {
            return creator.apply(rawValue);
        }
    }

    String getStyleName();

    StyleValue<VALUE> createStyleValue(String rawValue);

    default Optional<VALUE> parse(Map<String, StyleValue<?>> properties) {
        if (properties.containsKey(getStyleName())) {
            try {
                return (Optional<VALUE>) Optional.ofNullable(properties.get(getStyleName()).compute());
            } catch (Exception ignored) {}
        }
        return Optional.empty();
    }
}
