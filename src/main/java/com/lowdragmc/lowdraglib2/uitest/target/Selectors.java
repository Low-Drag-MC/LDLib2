package com.lowdragmc.lowdraglib2.uitest.target;

/**
 * Validation for the CSS-like selector strings understood by
 * {@link com.lowdragmc.lowdraglib2.gui.ui.style.HierarchicalStyleMatcher}.
 *
 * <p>The matcher's tokeniser has no alternation for {@code ,}, {@code [} or {@code (}, and it drives
 * the scan with {@code Matcher#find()} — so unrecognised characters are silently skipped rather than
 * rejected. {@code "#a, #b"} therefore parses as the descendant selector {@code "#a #b"} and quietly
 * matches nothing, which in a test reads as "the element disappeared" rather than "your selector is
 * not supported". Failing loudly here is the whole point of this class.
 */
public final class Selectors {

    private Selectors() {
    }

    /**
     * Supported: {@code tag}, {@code .class}, {@code #id}, {@code *}, descendant (space), child
     * ({@code >}), {@code :not(...)}, and {@code :state} (desugared to {@code .__state__}).
     *
     * @throws IllegalArgumentException with a message naming the alternative, for syntax the engine
     *                                  would mis-parse instead of reject
     */
    public static String validate(String selector) {
        if (selector == null || selector.isBlank()) {
            throw new IllegalArgumentException("Selector must not be blank");
        }
        if (selector.indexOf(',') >= 0) {
            throw new IllegalArgumentException(
                    "Selector lists are not supported: '" + selector + "' would silently parse as the "
                            + "descendant selector '" + selector.replace(",", "") + "'. "
                            + "Query each selector separately, or use ctx.query(..).where(..).");
        }
        if (selector.indexOf('[') >= 0) {
            throw new IllegalArgumentException(
                    "Attribute selectors are not supported: '" + selector + "'. "
                            + "Use ctx.query(..).withText(..) / .withId(..) / .where(..) instead.");
        }
        int open = selector.indexOf('(');
        if (open >= 0 && !isOnlyNotParentheses(selector)) {
            throw new IllegalArgumentException(
                    "The only functional pseudo-class supported is :not(...) — got '" + selector + "'. "
                            + "For :nth-child use ctx.query(..).nth(index).");
        }
        return selector;
    }

    private static boolean isOnlyNotParentheses(String selector) {
        for (int i = selector.indexOf('('); i >= 0; i = selector.indexOf('(', i + 1)) {
            if (!selector.regionMatches(true, Math.max(0, i - 4), ":not", 0, 4)) {
                return false;
            }
        }
        return true;
    }
}
