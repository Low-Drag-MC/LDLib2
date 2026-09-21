package com.lowdragmc.lowdraglib2.editor.keymap;

import java.util.function.Predicate;

/**
 * When an action is allowed to run — the {@code when} clause of a binding.
 *
 * <p>Two actions may share a chord as long as their contexts differ; the more specific one is tried
 * first, and if it declines the next takes its turn. {@link #specificity()} is what orders them: a
 * context that asks for focus in one panel beats one that applies to the whole editor, whatever order
 * they were registered in.
 */
@FunctionalInterface
public interface KeyContext {
    /** Applies everywhere in the editor. */
    int SPECIFICITY_GLOBAL = 0;
    /** Depends on the editor's state, but not on which panel the user is in. */
    int SPECIFICITY_STATE = 10;
    /** Depends on where the focus is. */
    int SPECIFICITY_FOCUS = 100;

    boolean test(KeymapContext context);

    /** How specific this context is; higher wins when two actions want the same chord. */
    default int specificity() {
        return SPECIFICITY_GLOBAL;
    }

    static KeyContext global() {
        return context -> true;
    }

    static KeyContext of(Predicate<KeymapContext> predicate, int specificity) {
        return new KeyContext() {
            @Override
            public boolean test(KeymapContext context) {
                return predicate.test(context);
            }

            @Override
            public int specificity() {
                return specificity;
            }
        };
    }

    /** Focus is on an element of this type, or inside one. */
    static KeyContext focusWithin(Class<?> elementType) {
        return of(context -> context.isFocusWithin(elementType), SPECIFICITY_FOCUS);
    }

    /** A project is open. Actions that act on the project are pointless without one. */
    static KeyContext withProject() {
        return of(KeymapContext::hasProject, SPECIFICITY_STATE);
    }

    /** Focus is not in a text field, text area or code editor. */
    static KeyContext notTyping() {
        return of(context -> !context.isTextInputFocused(), SPECIFICITY_STATE);
    }

    /** Both contexts hold. As specific as the more specific of the two. */
    default KeyContext and(KeyContext other) {
        return of(context -> test(context) && other.test(context),
                Math.max(specificity(), other.specificity()));
    }
}
