package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;

import java.util.*;
import java.util.regex.Pattern;

@KJSBindings
public final class Stylesheet {
    public static final Stylesheet EMPTY = new Stylesheet(Collections.emptyList());

    public static final Pattern RULE = Pattern.compile("(?s)([^{]+)\\{([^}]*)}");
    public static final Pattern DECL = Pattern.compile("(?m)\\s*([\\w-]+)\\s*:\\s*([^;]+)\\s*;?");

    public final List<StyleRule> rules = new ArrayList<>();

    public Stylesheet(List<StyleRule> rules) {
        this.rules.addAll(rules);
    }

    public void addRule(StyleRule rule) {
        rules.add(rule);
    }

    public void removeRule(StyleRule rule) {
        rules.remove(rule);
    }

    public void clear() {
        rules.clear();
    }

    public void merge(Stylesheet other) {
        for (StyleRule r : other.rules) {
            addRule(r);
        }
    }

    public List<StyleRule> calculateValues(UIElement element) {
        var matchRules = new ArrayList<StyleRule>();
        for (StyleRule rule : rules) {
            if (rule.matches(element)) {
                matchRules.add(rule);
            }
        }
        return Collections.unmodifiableList(matchRules);
    }

    public static Stylesheet parse(String rawStylesheet) {
        // remove single-line comments
        rawStylesheet = rawStylesheet.replaceAll("//.*", "");
        // remove multi-line comments
        rawStylesheet = rawStylesheet.replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");

        var m = RULE.matcher(rawStylesheet);
        var rules = new ArrayList<StyleRule>();
        while (m.find()) {
            var selectorText = m.group(1).trim();
            var block = m.group(2);

            var properties = parseStyleValues(block);
            var immutableProperties = Collections.unmodifiableMap(properties);
            for (var rawM : selectorText.split(",")) {
                try {
                    rules.add(new StyleRule(HierarchicalStyleMatcher.parse(rawM.trim()), immutableProperties));
                } catch (Exception ignored) {}
            }
        }
        return new Stylesheet(rules);
    }

    public static Map<Property<?>, StyleValue<?>> parseStyleValues(String block) {
        Map<Property<?>, StyleValue<?>> properties = new HashMap<>();
        var m2 = DECL.matcher(block);
        while (m2.find()) {
            var name = m2.group(1).trim();
            var rawValue = m2.group(2).trim();
            var p = PropertyRegistry.byName(name);
            if (p != null) {
                try {
                    var value = p.valueParser.parse(rawValue);
                    properties.put(p, value);
                } catch (Exception ignored) {}
            }
        }
        return properties;
    }
}