package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Hierarchical style matcher that supports inheritance relationships, supporting the following syntax:
 * - "div span" - Descendant selector: all span descendants of div
 * - "div > span" - Child selector: direct span children of div
 */
public record HierarchicalStyleMatcher(List<SelectorGroup> selectorGroups) {

    // Match selectors and combinators: element, class, ID, or >
    private static final Pattern SELECTOR_PATTERN = Pattern.compile(
            "([a-zA-Z0-9_-]+|[#.][a-zA-Z0-9_-]+)|(>)|(\\s+)"
    );

    /**
     * Parse selector string
     * For example: ".class1 > div.class2 span#id"
     */
    public static HierarchicalStyleMatcher parse(String selectorString) {
        if (selectorString == null || selectorString.trim().isEmpty()) {
            throw new IllegalArgumentException("Selector cannot be null or empty");
        }

        var matcher = SELECTOR_PATTERN.matcher(selectorString.trim());
        List<SelectorGroup> groups = new ArrayList<>();
        List<StyleSelector> currentSelectors = new ArrayList<>();

        boolean nextIsChild = false;
        boolean expectingSelector = true;

        while (matcher.find()) {
            String g1 = matcher.group(1); // simple selector: tag / .class / #id
            String g2 = matcher.group(2); // '>'
            String g3 = matcher.group(3); // spaces

            if (g1 != null) {
                // save selector
                currentSelectors.add(StyleSelector.parse(g1));
                expectingSelector = false;
                continue;
            }

            // check child or descendant combinator
            if (g2 != null || g3 != null) {
                if (expectingSelector) {
                    // check valid
                    throw new IllegalArgumentException("Invalid selector near: " + matcher.group());
                }
                // 把“当前组”按“nextIsChild(与上一组的关系)”落盘
                // make current group follow `nextIsChild`
                if (!currentSelectors.isEmpty()) {
                    groups.add(new SelectorGroup(StyleMatcher.create(currentSelectors), nextIsChild));
                    currentSelectors.clear();
                }
                // setup next group
                nextIsChild = (g2 != null); // '>' -> child；空格 -> descendant(false)
                expectingSelector = true;   // 组合符后面必须跟选择器
            }
        }

        // check valid
        if (currentSelectors.isEmpty()) {
            throw new IllegalArgumentException("Selector cannot end with a combinator: " + selectorString);
        }
        groups.add(new SelectorGroup(StyleMatcher.create(currentSelectors), nextIsChild));

        return new HierarchicalStyleMatcher(groups);
    }

    /**
     * check if the element matches the entire selector chain
     */
    public boolean matches(UIElement element) {
        if (element == null || selectorGroups.isEmpty()) {
            return false;
        }

        return matchesRecursively(element, selectorGroups.size() - 1);
    }

    private boolean matchesRecursively(UIElement element, int groupIndex) {
        if (groupIndex < 0) {
            return true; // match success
        }

        SelectorGroup group = selectorGroups.get(groupIndex);

        // check current element matches current group's selectors'
        if (!group.styleMatcher.matches(element)) {
            return false;
        }

        if (groupIndex == 0) {
            return true; // success
        }

        // Check the previous element that matches the previous group's selectors'
        if (group.isChildCombinator) {
            // only check parent element
            UIElement parent = element.getParent();
            return parent != null && matchesRecursively(parent, groupIndex - 1);
        } else {
            // check all ancestors
            UIElement current = element.getParent();
            while (current != null) {
                if (matchesRecursively(current, groupIndex - 1)) {
                    return true;
                }
                current = current.getParent();
            }
            return false;
        }
    }

    /**
     * calculates the specificity of the selector
     */
    public int getSpecificity() {
        return selectorGroups.stream()
                .mapToInt(group -> group.styleMatcher.weight())
                .sum();
    }

    @Override
    public String toString() {
        if (selectorGroups.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectorGroups.size(); i++) {
            SelectorGroup group = selectorGroups.get(i);

            if (i > 0) {
                if (group.isChildCombinator) {
                    sb.append(" > ");
                } else {
                    sb.append(" ");
                }
            }

            sb.append(group.styleMatcher.toString());
        }
        return sb.toString();
    }

    /**
     * Selector group containing multiple selectors and combinator type
     *
     * @param isChildCombinator false = descendant selector，true = child selector
     */

    public record SelectorGroup(StyleMatcher styleMatcher, boolean isChildCombinator) {
    }
}