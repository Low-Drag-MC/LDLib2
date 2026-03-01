package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

@KJSBindings
public final class Stylesheet {
    @Setter @Getter
    private String name = "unknown";
    @Getter @Nullable
    private String rawLss;
    public static final Pattern RULE = Pattern.compile("(?s)([^{]+)\\{([^}]*)}");
    public static final Pattern DECL = Pattern.compile("(?m)\\s*([\\w-]+)\\s*:\\s*([^;]+)\\s*;?");

    public final List<StyleRule> rules = new ArrayList<>();
    private final boolean immutable;

    // Bucketed indexes for fast selector matching (rebuilt when rules change)
    private final Map<String, List<StyleRule>> byElementName = new HashMap<>();
    private final Map<String, List<StyleRule>> byClassName = new HashMap<>();
    private final Map<String, List<StyleRule>> byId = new HashMap<>();
    private final List<StyleRule> universalRules = new ArrayList<>();

    public Stylesheet(List<StyleRule> rules) {
        this.rules.addAll(rules);
        this.immutable = false;
        rebuildIndex();
    }

    private Stylesheet(List<StyleRule> rules, boolean immutable) {
        this.rules.addAll(rules);
        this.immutable = immutable;
        rebuildIndex();
    }

    public static final Stylesheet EMPTY = new Stylesheet(Collections.emptyList(), true);

    public void addRule(StyleRule rule) {
        if (immutable) throw new UnsupportedOperationException("Cannot modify immutable Stylesheet.EMPTY");
        rules.add(rule);
        indexRule(rule);
    }

    public void removeRule(StyleRule rule) {
        if (immutable) throw new UnsupportedOperationException("Cannot modify immutable Stylesheet.EMPTY");
        rules.remove(rule);
        rebuildIndex();
    }

    public void clear() {
        if (immutable) throw new UnsupportedOperationException("Cannot modify immutable Stylesheet.EMPTY");
        rules.clear();
        clearIndex();
    }

    public void merge(Stylesheet other) {
        if (immutable) throw new UnsupportedOperationException("Cannot modify immutable Stylesheet.EMPTY");
        for (StyleRule r : other.rules) {
            addRule(r);
        }
    }

    // --- Index management ---

    private void clearIndex() {
        byElementName.clear();
        byClassName.clear();
        byId.clear();
        universalRules.clear();
    }

    private void rebuildIndex() {
        clearIndex();
        for (StyleRule rule : rules) {
            indexRule(rule);
        }
    }

    private void indexRule(StyleRule rule) {
        var groups = rule.matcher.getSelectorGroups();
        if (groups.isEmpty()) {
            universalRules.add(rule);
            return;
        }
        var lastGroup = groups.getLast();
        var selectors = lastGroup.styleMatcher().selector();
        StyleSelector bucketSelector = null;
        for (StyleSelector s : selectors) {
            if (s.type() != SelectorType.UNIVERSAL && s.type() != SelectorType.NOT) {
                bucketSelector = s;
                break;
            }
        }
        if (bucketSelector == null) {
            universalRules.add(rule);
            return;
        }
        switch (bucketSelector.type()) {
            case ELEMENT -> byElementName.computeIfAbsent(
                    bucketSelector.identity().left().orElseThrow(), k -> new ArrayList<>()).add(rule);
            case CLASS -> byClassName.computeIfAbsent(
                    bucketSelector.identity().left().orElseThrow(), k -> new ArrayList<>()).add(rule);
            case ID -> byId.computeIfAbsent(
                    bucketSelector.identity().left().orElseThrow(), k -> new ArrayList<>()).add(rule);
            default -> universalRules.add(rule);
        }
    }

    // --- Matching ---

    public List<StyleRule> calculateValues(UIElement element) {
        // Gather candidates from buckets - use LinkedHashSet to preserve insertion order and deduplicate
        Set<StyleRule> candidates = new LinkedHashSet<>(universalRules);
        var byName = byElementName.get(element.getElementName());
        if (byName != null) candidates.addAll(byName);
        for (var cls : element.getClasses()) {
            var byCls = byClassName.get(cls);
            if (byCls != null) candidates.addAll(byCls);
        }
        var id = element.getId();
        if (!id.isEmpty()) {
            var byIdRules = byId.get(id);
            if (byIdRules != null) candidates.addAll(byIdRules);
        }
        // Full match on candidate set only
        var matchRules = new ArrayList<StyleRule>();
        for (StyleRule rule : candidates) {
            if (rule.matches(element)) {
                matchRules.add(rule);
            }
        }
        return Collections.unmodifiableList(matchRules);
    }

    public static Stylesheet parse(String rawStylesheet) {
        var raw = rawStylesheet.trim();
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
                } catch (Exception e) {
                    LDLib2.LOGGER.warn("Invalid selector '{}': {}", rawM.trim(), e.getMessage());
                }
            }
        }
        var ss = new Stylesheet(rules);
        ss.rawLss = raw;
        return ss;
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
                } catch (Exception e) {
                    LDLib2.LOGGER.warn("Failed to parse value '{}' for property '{}': {}", rawValue, name, e.getMessage());
                }
            } else {
                LDLib2.LOGGER.debug("Unknown style property: '{}'", name);
            }
        }
        return properties;
    }
}
