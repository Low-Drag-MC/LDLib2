package com.lowdragmc.lowdraglib.gui.ui.style;

import java.util.ArrayList;
import java.util.List;

public final class StyleSheet {
    public final List<StyleRule> rules = new ArrayList<>();

    public void addRule(StyleRule rule) {
        rules.add(rule);
    }

    public void removeRule(StyleRule rule) {
        rules.remove(rule);
    }

    public void clear() {
        rules.clear();
    }

    public void merge(StyleSheet other) {
        rules.addAll(other.rules);
    }
}