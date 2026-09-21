package com.lowdragmc.lowdraglib2.editor.keymap;

import com.lowdragmc.lowdraglib2.gui.ui.utils.KeyState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A chord is read from the modifier bits the key event carries, not from polling a window for what is
 * held. That is what lets a shortcut work in an editor torn off into its own operating-system window,
 * and it is the one part of the keymap the in-client suite cannot cover on every platform — the
 * command-key path only exists on a Mac.
 */
class KeyChordFromEventTest {

    @AfterEach
    void clearSource() {
        KeyState.setSource(null);
    }

    @Test
    void theEventsOwnBitsBecomeTheChord() {
        var chord = KeyChord.fromModifiers(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT);
        assertEquals(KeyChord.ctrlShift(GLFW.GLFW_KEY_S), chord);
        assertTrue(chord.isCtrlDown());
        assertTrue(chord.isShiftDown());
        assertFalse(chord.isAltDown());
    }

    @Test
    void noModifierBitsIsABareKey() {
        assertEquals(KeyChord.key(GLFW.GLFW_KEY_F5), KeyChord.fromModifiers(GLFW.GLFW_KEY_F5, 0));
        assertTrue(KeyChord.fromModifiers(GLFW.GLFW_KEY_F5, 0).hasNoModifier());
    }

    /**
     * Caps lock and num lock ride along in a GLFW modifier mask and mean nothing to a shortcut. A chord
     * that took them in would stop matching the moment a user left caps lock on.
     */
    @Test
    void lockKeysAreNotPartOfAChord() {
        var chord = KeyChord.fromModifiers(GLFW.GLFW_KEY_S,
                GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_CAPS_LOCK | GLFW.GLFW_MOD_NUM_LOCK);
        assertEquals(KeyChord.ctrl(GLFW.GLFW_KEY_S), chord);
    }

    /**
     * ⚠️ The command key is folded onto ctrl, so a binding reads "ctrl+s" in the settings file on every
     * platform and means whichever key that platform uses for shortcuts. Under an installed
     * {@link KeyState.Source} either bit counts — a test that presses control is asking for the
     * shortcut, and which key the running machine would use is beside the point.
     */
    @Test
    void underATestSourceEitherCommandOrControlMakesACtrlChord() {
        KeyState.setSource(key -> false);
        assertEquals(KeyChord.ctrl(GLFW.GLFW_KEY_S),
                KeyChord.fromModifiers(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL));
        assertEquals(KeyChord.ctrl(GLFW.GLFW_KEY_S),
                KeyChord.fromModifiers(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_SUPER));
    }

    /**
     * With no source and no client — a dedicated server, or this test — the platform bit is control, so
     * a stray super bit is not a shortcut. On Windows and Linux that is the real answer too: super is
     * the windows key, and Win+S belongs to the desktop.
     */
    @Test
    void withNoSourceTheSuperBitAloneIsNotACtrlChord() {
        assertFalse(KeyChord.fromModifiers(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_SUPER).isCtrlDown());
        assertTrue(KeyChord.fromModifiers(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL).isCtrlDown());
    }

    /** A modifier is never a chord on its own: holding shift must not fire whatever shift is bound to. */
    @Test
    void aModifierKeyOnItsOwnIsNotAChord() {
        assertFalse(KeyChord.fromModifiers(GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_MOD_CONTROL).isBound());
        assertFalse(KeyChord.fromModifiers(GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_MOD_SHIFT).isBound());
    }

    /**
     * The bit a modifier's own press implies. The chord capture field adds it in rather than trusting
     * the event, because GLFW is not consistent across platforms about whether a modifier's press
     * event already carries its own bit.
     */
    @Test
    void everyModifierKeyMapsToItsBit() {
        assertEquals(GLFW.GLFW_MOD_SHIFT, KeyState.modifierBitOf(GLFW.GLFW_KEY_LEFT_SHIFT));
        assertEquals(GLFW.GLFW_MOD_SHIFT, KeyState.modifierBitOf(GLFW.GLFW_KEY_RIGHT_SHIFT));
        assertEquals(GLFW.GLFW_MOD_CONTROL, KeyState.modifierBitOf(GLFW.GLFW_KEY_LEFT_CONTROL));
        assertEquals(GLFW.GLFW_MOD_CONTROL, KeyState.modifierBitOf(GLFW.GLFW_KEY_RIGHT_CONTROL));
        assertEquals(GLFW.GLFW_MOD_ALT, KeyState.modifierBitOf(GLFW.GLFW_KEY_LEFT_ALT));
        assertEquals(GLFW.GLFW_MOD_ALT, KeyState.modifierBitOf(GLFW.GLFW_KEY_RIGHT_ALT));
        assertEquals(GLFW.GLFW_MOD_SUPER, KeyState.modifierBitOf(GLFW.GLFW_KEY_LEFT_SUPER));
        assertEquals(0, KeyState.modifierBitOf(GLFW.GLFW_KEY_S));
    }

    /** What the capture field commits has to survive the round trip through the settings file. */
    @Test
    void aCapturedChordRoundTripsThroughItsTextForm() {
        var captured = KeyChord.fromModifiers(GLFW.GLFW_KEY_PAGE_DOWN,
                GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT);
        assertEquals(captured, KeyChord.parse(captured.serialize()).orElseThrow());
    }
}
