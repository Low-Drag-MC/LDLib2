package com.lowdragmc.lowdraglib2.editor.keymap;

/**
 * What an action's {@link KeyContext} gets to ask about the moment a chord was pressed.
 *
 * <p>Deliberately a set of questions rather than a handle on the editor: a handler that needs the
 * editor captures it where the action is registered, which keeps this whole package free of UI types
 * — and therefore testable without a running game.
 */
public interface KeymapContext {

    /** The chord that was pressed. */
    KeyChord chord();

    /**
     * Whether the focused element is of this type or sits inside one — the editor walks the focus chain.
     *
     * <p>This is how an action is scoped to a panel: the graph editor's delete and the asset browser's
     * delete are both bound to {@code Delete} and neither has to know about the other.
     */
    boolean isFocusWithin(Class<?> elementType);

    /**
     * Whether focus is in something the user types into.
     *
     * <p>A text field already stops the keys it owns from getting this far, so this is for the rest: an
     * action bound to a bare key must not fire while a field that ignores that key has focus.
     */
    boolean isTextInputFocused();

    /** Whether the editor currently has a project open. */
    boolean hasProject();
}
