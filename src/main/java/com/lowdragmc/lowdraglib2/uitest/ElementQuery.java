package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.uitest.target.Selectors;
import com.lowdragmc.lowdraglib2.uitest.target.Texts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A composable filter over {@link ModularUI#select(String)}, covering everything the CSS engine
 * cannot express: element type, visible text, ordinal position, and arbitrary predicates.
 *
 * <p>Reach for this instead of stretching a selector string. The matcher has no attribute selectors
 * and no {@code :nth-child}, and {@link Selectors} rejects both rather than let them mis-parse.
 *
 * <p>Every filter is applied in the order it was added, and the whole chain is evaluated lazily when
 * a terminal ({@link #one()}, {@link #list()}, {@link #count()}) is called — so a query built once
 * and reused across steps always reflects the current tree.
 */
public final class ElementQuery {

    private final ModularUI modularUI;
    private final List<Predicate<UIElement>> filters = new ArrayList<>();
    private final StringBuilder description = new StringBuilder();
    private String selector = "*";
    private int nth = -1;
    private boolean excludingInternal;

    ElementQuery(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    /** Seeds the candidate set. Without this the query starts from every registered element. */
    public ElementQuery select(String selector) {
        this.selector = Selectors.validate(selector);
        describe("select(" + selector + ")");
        return this;
    }

    public ElementQuery type(Class<? extends UIElement> type) {
        filters.add(type::isInstance);
        describe("type(" + type.getSimpleName() + ")");
        return this;
    }

    public ElementQuery withId(String id) {
        filters.add(element -> element.getId().equals(id));
        describe("id(" + id + ")");
        return this;
    }

    public ElementQuery withIdMatching(String regex) {
        var pattern = Pattern.compile(regex);
        filters.add(element -> pattern.matcher(element.getId()).matches());
        describe("idMatching(" + regex + ")");
        return this;
    }

    public ElementQuery withText(String exactText) {
        excludeInternalOnce();
        filters.add(element -> Texts.of(element).equals(exactText));
        describe("text(" + exactText + ")");
        return this;
    }

    public ElementQuery withTextContaining(String part) {
        excludeInternalOnce();
        filters.add(element -> Texts.of(element).contains(part));
        describe("textContains(" + part + ")");
        return this;
    }

    public ElementQuery withClass(String styleClass) {
        filters.add(element -> element.hasClass(styleClass));
        describe("class(" + styleClass + ")");
        return this;
    }

    public ElementQuery visible() {
        filters.add(element -> element.isVisible() && element.isDisplayed());
        describe("visible");
        return this;
    }

    public ElementQuery active() {
        filters.add(UIElement::isActive);
        describe("active");
        return this;
    }

    /**
     * Drops elements a composite built for itself — a {@link com.lowdragmc.lowdraglib2.gui.ui.elements.Button}
     * registers its caption {@code TextElement}, so a text query would otherwise match the inner
     * label rather than the button you meant to click. Applied automatically by the text filters.
     */
    public ElementQuery excludeInternal() {
        excludeInternalOnce();
        return this;
    }

    /** Idempotent, so two text filters do not stack two identical predicates and two description entries. */
    private void excludeInternalOnce() {
        if (excludingInternal) return;
        excludingInternal = true;
        filters.add(element -> !element.isInternalUI());
        describe("excludeInternal");
    }

    public ElementQuery where(Predicate<UIElement> predicate) {
        filters.add(predicate);
        describe("where(..)");
        return this;
    }

    /** Keeps only the element at this index of the surviving matches. */
    public ElementQuery nth(int index) {
        this.nth = index;
        describe("nth(" + index + ")");
        return this;
    }

    // region terminals

    public List<ElementRef> list() {
        var matches = matches();
        var refs = new ArrayList<ElementRef>(matches.size());
        for (var element : matches) {
            refs.add(new ElementRef(element, describeSelf()));
        }
        return refs;
    }

    public int count() {
        return matches().size();
    }

    public Optional<ElementRef> optional() {
        var matches = matches();
        return matches.size() == 1
                ? Optional.of(new ElementRef(matches.getFirst(), describeSelf()))
                : Optional.empty();
    }

    /**
     * @throws IllegalStateException when the query matches zero or more than one element. Both are
     *         mistakes worth failing on: zero means the assumption about the UI is wrong, and more
     *         than one means the step would silently pick an arbitrary element.
     */
    public ElementRef one() {
        var matches = matches();
        if (matches.isEmpty()) {
            throw new IllegalStateException("No element matched " + describeSelf());
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("Expected exactly one element for " + describeSelf()
                    + " but found " + matches.size() + ": " + preview(matches)
                    + ". Narrow it with .nth(i) or another filter.");
        }
        return new ElementRef(matches.getFirst(), describeSelf());
    }

    // endregion

    private List<UIElement> matches() {
        var stream = modularUI.select(selector);
        for (var filter : filters) {
            stream = stream.filter(filter);
        }
        var matches = stream.toList();
        if (nth >= 0) {
            return nth < matches.size() ? List.of(matches.get(nth)) : List.of();
        }
        return matches;
    }

    private static String preview(List<UIElement> matches) {
        return matches.stream().limit(5)
                .map(element -> element.getElementName()
                        + (element.getId().isEmpty() ? "" : "#" + element.getId()))
                .toList() + (matches.size() > 5 ? " ..." : "");
    }

    private void describe(String part) {
        if (!description.isEmpty()) description.append('.');
        description.append(part);
    }

    String describeSelf() {
        return "query[" + description + "]";
    }
}
