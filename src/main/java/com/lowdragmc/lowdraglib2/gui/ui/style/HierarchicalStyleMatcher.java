package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.mojang.datafixers.util.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Hierarchical style matcher that supports inheritance relationships, supporting the following syntax:
 * - "div span" - Descendant selector: all span descendants of div
 * - "div > span" - Child selector: direct span children of div
 * <p>
 * Supports CSS state pseudo-class sugar: any {@code :xxx} pseudo-class (that is not a scope modifier)
 * is automatically translated to the internal class {@code __xxx__}.
 * Examples:
 * <pre>
 *   button:hover          →  button.__hover__
 *   button:hover:focus    →  button.__hover__.__focus__
 *   .panel:disabled       →  .panel.__disabled__
 *   button:my-state       →  button.__my-state__   (custom state, no hardcoded map needed)
 * </pre>
 * Scope modifiers ({@code :host}, {@code :internal}) are NOT converted to classes and continue
 * to work as before.
 * <p>
 * In {@link StyleSelector#toString()}, {@code __xxx__} class names are printed back as {@code :xxx}.
 */
public class HierarchicalStyleMatcher {

    /** Pseudo-class names that act as scope modifiers rather than state classes. */
    private static final Set<String> SCOPE_PSEUDO_CLASSES = Set.of("host", "internal");

    private final List<SelectorGroup> selectorGroups;

    // runtime
    private int specificity = -1;

    public HierarchicalStyleMatcher(List<SelectorGroup> selectorGroups) {
        this.selectorGroups = selectorGroups;
    }

    public List<SelectorGroup> getSelectorGroups() {
        return selectorGroups;
    }

    // Match selectors and combinators: element, class, ID, or >
    // Updated to support multiple pseudo-class suffixes (e.g. button:hover:focus)
    private static final Pattern SELECTOR_PATTERN = Pattern.compile(
            ":not\\((.*?)\\)|((?:[a-zA-Z0-9*_-]+|[#.][a-zA-Z0-9_-]+)(?::(?!not\\()[a-zA-Z0-9_-]+)*)|(>)|(\\s+)"
    );

    /**
     * Parse selector string.
     * For example: ".class1 > div.class2 span#id"
     * Supports state pseudo-classes: "button:hover", ".panel:focus", "button:hover:disabled"
     */
    public static HierarchicalStyleMatcher parse(String selectorString) throws IllegalArgumentException {
        if (selectorString == null || selectorString.trim().isEmpty()) {
            throw new IllegalArgumentException("Selector cannot be null or empty");
        }

        var matcher = SELECTOR_PATTERN.matcher(selectorString.trim());
        List<SelectorGroup> groups = new ArrayList<>();
        List<StyleSelector> currentSelectors = new ArrayList<>();

        boolean nextIsChild = false;
        boolean expectingSelector = true;

        while (matcher.find()) {
            var not = matcher.group(1); // :not(...)
            var selector = matcher.group(2); // simple selector: tag / .class / #id (with optional pseudo-classes)
            var child = matcher.group(3); // '>'
            var space = matcher.group(4); // spaces

            if (not != null) {
                currentSelectors.add(StyleSelector.parseNotSelector(not));
                expectingSelector = false;
                continue;
            }

            if (selector != null) {
                parseSelectorWithPseudo(selector, currentSelectors);
                expectingSelector = false;
                continue;
            }

            // check child or descendant combinator
            if (child != null || space != null) {
                if (!currentSelectors.isEmpty()) {
                    groups.add(new SelectorGroup(StyleMatcher.create(currentSelectors), nextIsChild));
                    currentSelectors.clear();
                }

                if (space != null) {
                    continue;
                }

                if (expectingSelector) {
                    throw new IllegalArgumentException("Invalid selector near: " + matcher.group());
                }

                nextIsChild = true;
                expectingSelector = true;
            }
        }

        if (currentSelectors.isEmpty()) {
            throw new IllegalArgumentException("Selector cannot end with a combinator: " + selectorString);
        }
        groups.add(new SelectorGroup(StyleMatcher.create(currentSelectors), nextIsChild));

        return new HierarchicalStyleMatcher(groups);
    }

    /**
     * Parse a single selector token that may contain pseudo-class suffixes.
     * e.g. "button:hover:focus" → [ELEMENT("button"), CLASS("__hover__"), CLASS("__focus__")]
     * e.g. "button:host" → [ELEMENT("button", HOST scope)]
     * e.g. "button" → [ELEMENT("button")]
     */
    private static void parseSelectorWithPseudo(String selector, List<StyleSelector> out) {
        int firstColon = selector.indexOf(':');
        if (firstColon <= 0) {
            out.add(StyleSelector.parse(selector));
            return;
        }

        String base = selector.substring(0, firstColon);
        String[] pseudos = selector.substring(firstColon + 1).split(":");

        SelectorScope scope = SelectorScope.ALL;
        List<String> stateClasses = new ArrayList<>();

        for (String pseudo : pseudos) {
            if (SCOPE_PSEUDO_CLASSES.contains(pseudo)) {
                scope = switch (pseudo) {
                    case "host"     -> SelectorScope.HOST;
                    case "internal" -> SelectorScope.INTERNAL;
                    default         -> scope;
                };
            } else {
                if (pseudo.equals("hover")) {
                    pseudo = "hovered"; // compat to css
                }
                // Generic rule: any :xxx (non-scope pseudo-class) → __xxx__ class
                stateClasses.add("__" + pseudo + "__");
            }
        }

        // Add the base selector with the resolved scope
        out.add(parsePureSelector(base, scope));

        // Add state class selectors
        for (String stateClass : stateClasses) {
            out.add(new StyleSelector(SelectorType.CLASS, Either.left(stateClass), SelectorScope.ALL));
        }
    }

    /** Parse a base selector string (no pseudo-class) with an explicit scope. */
    private static StyleSelector parsePureSelector(String base, SelectorScope scope) {
        base = base.trim();
        if (base.startsWith(".")) {
            return new StyleSelector(SelectorType.CLASS, Either.left(base.substring(1)), scope);
        } else if (base.startsWith("#")) {
            return new StyleSelector(SelectorType.ID, Either.left(base.substring(1)), scope);
        } else if (base.equals("*")) {
            return new StyleSelector(SelectorType.UNIVERSAL, Either.left("*"), scope);
        } else {
            return new StyleSelector(SelectorType.ELEMENT, Either.left(base), scope);
        }
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

        if (!group.styleMatcher.matches(element)) {
            return false;
        }

        if (groupIndex == 0) {
            return true;
        }

        if (group.isChildCombinator) {
            UIElement parent = element.getParent();
            return parent != null && matchesRecursively(parent, groupIndex - 1);
        } else {
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
        return specificity == -1 ? (specificity = selectorGroups.stream()
                .mapToInt(group -> group.styleMatcher.weight())
                .sum()) : specificity;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HierarchicalStyleMatcher that)) return false;
        return Objects.equals(selectorGroups, that.selectorGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(selectorGroups);
    }

    /**
     * Selector group containing multiple selectors and combinator type
     *
     * @param isChildCombinator false = descendant selector，true = child selector
     */
    public record SelectorGroup(StyleMatcher styleMatcher, boolean isChildCombinator) {
    }
}