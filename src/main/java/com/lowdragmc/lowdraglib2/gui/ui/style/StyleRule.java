package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class StyleRule {
    private final List<HierarchicalStyleMatcher> matchers = new ArrayList<>();
    private final Map<String, StyleValue<?>> properties = new ConcurrentHashMap<>();

    public void addMatcher(HierarchicalStyleMatcher matcher) {
        matchers.add(matcher);
    }

    public void addProperty(String name, StyleValue<?> value) {
        properties.put(name, value);
    }

    public StyleValue<?> getProperty(String name) {
        return properties.get(name);
    }

    public boolean matches(UIElement element) {
        return matchers.stream().anyMatch(m -> m.matches(element));
    }
}