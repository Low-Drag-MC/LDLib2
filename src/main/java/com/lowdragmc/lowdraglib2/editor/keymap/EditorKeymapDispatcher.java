package com.lowdragmc.lowdraglib2.editor.keymap;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Turns key presses inside an editor into {@link EditorAction}s.
 *
 * <p>Listens on the editor element in the <b>bubble</b> phase, which is what keeps the keymap out of the
 * way of everything below it: an element that consumed the key — a text field typing a character, a
 * dialog closing on Escape — has already stopped the event, and the keymap never sees it. The cost is
 * that a shortcut cannot override a focused control, which is the right way round.
 *
 * <p>The chord comes from the modifier bits on the event itself ({@link KeyChord#fromEvent}), not from
 * polling a window for what is held right now. On this branch the event is the better source in every
 * case that differs: an editor torn off into its own operating-system window gets its bits from that
 * window's own GLFW callback, the test driver states them outright, and they describe the moment of the
 * press rather than whenever the handler got round to asking. It is also what the game itself reads for
 * {@code KeyEvent#isCopy} and friends, so a keymap chord and a built-in command chord now agree by
 * construction.
 */
public class EditorKeymapDispatcher implements KeymapContext {
    @Getter
    private final Editor editor;

    // runtime, valid only for the duration of one dispatch
    private KeyChord chord = KeyChord.UNBOUND;
    @Nullable
    private UIElement focused;

    public EditorKeymapDispatcher(Editor editor) {
        this.editor = editor;
    }

    public void install() {
        editor.addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);
    }

    protected void onKeyDown(UIEvent event) {
        var chord = KeyChord.fromEvent(event);
        if (!chord.isBound()) return;
        // Claimed before anything else is decided: from here on this chord means whatever this keymap
        // says it means, including nothing at all. Without this, ModularUI's built-in table would still
        // run Ctrl+S as save after the user moved save onto another key.
        event.keymapResolved = true;
        this.chord = chord;
        this.focused = resolveFocused();
        // The focused control gets the last word on keys it uses. Text fields already stop those from
        // reaching us, but an element that handles a key without stopping the event would otherwise have
        // its key stolen by a shortcut - and an element that is merely *about* to type a character has
        // nothing to stop yet at all.
        if (isKeyOwnedByFocus(event)) return;
        var action = editor.getKeymap().dispatch(this);
        if (action != null) {
            // Both matter: stopping keeps the key from reaching another shortcut further up (an editor
            // inside an editor window), and it is what tells ModularUI not to also run this chord
            // through its built-in command table.
            event.hasHandler = true;
            event.stopPropagation();
        }
    }

    @Nullable
    private UIElement resolveFocused() {
        var ui = editor.getModularUI();
        return ui == null ? null : ui.getFocusedElement();
    }

    private boolean isKeyOwnedByFocus(UIEvent event) {
        for (var element = focused; element != null; element = element.getParent()) {
            if (element.ownsKey(event)) return true;
            if (element == editor) break;
        }
        return false;
    }

    @Override
    public KeyChord chord() {
        return chord;
    }

    @Override
    public boolean isFocusWithin(Class<?> elementType) {
        for (var element = focused; element != null; element = element.getParent()) {
            if (elementType.isInstance(element)) return true;
        }
        return false;
    }

    @Override
    public boolean isTextInputFocused() {
        for (var element = focused; element != null; element = element.getParent()) {
            if (element.isTextInput()) return true;
            if (element == editor) break;
        }
        return false;
    }

    @Override
    public boolean hasProject() {
        return editor.getCurrentProject() != null;
    }

    /** The element that had focus when the current chord arrived. Null outside a dispatch. */
    @Nullable
    public UIElement getFocusedElement() {
        return focused;
    }
}
