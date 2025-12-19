package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record StyleMatcher(StyleSelector[] selector, int weight) {

    public static StyleMatcher create(StyleSelector[] selector) {
        return new StyleMatcher(selector, Arrays.stream(selector).mapToInt(StyleSelector::weight).sum());
    }

    public static StyleMatcher create(List<StyleSelector> selectors) {
        return create(selectors.toArray(new StyleSelector[0]));
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
