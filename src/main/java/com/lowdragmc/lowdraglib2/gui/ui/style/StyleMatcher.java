package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record StyleMatcher(StyleSelector[] selector, int weight) {
    public static Pattern PATTERN = Pattern.compile("(^[a-zA-Z0-9_-]+)|([#.][a-zA-Z0-9_-]+)");

    public static StyleMatcher create(StyleSelector[] selector) {
        return new StyleMatcher(selector, Arrays.stream(selector).mapToInt(StyleSelector::weight).sum());
    }

    public static StyleMatcher create(List<StyleSelector> selectors) {
        return create(selectors.toArray(new StyleSelector[0]));
    }

    public static StyleMatcher parse(String raw) {
        raw = raw.trim();
        if (raw.equals("*")) return create(new StyleSelector[]{StyleSelector.parse(raw)});
        var matcher = PATTERN.matcher(raw);
        List<StyleSelector> selectors = new ArrayList<>();
        while (matcher.find()) {
            var token = matcher.group();
            selectors.add(StyleSelector.parse(token));
        }
        return create(selectors);
    }

    public boolean matches(UIElement element) {
        for (var styleSelector : selector) {
            if (!styleSelector.matches(element)) return false;
        }
        return true;
    }

    @Override
    public @NotNull String toString() {
        return Arrays.stream(selector)
                .map(StyleSelector::toString)
                .collect(Collectors.joining(""));
    }
}
