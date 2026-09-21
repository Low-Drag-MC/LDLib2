package com.lowdragmc.lowdraglib2.editor.keymap;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The text form of a chord is a config format — it has to survive a round trip, and it has to refuse
 * input it cannot represent rather than silently binding the wrong key.
 */
class KeyChordTest {

    @Test
    void chordsRoundTripThroughTheirTextForm() {
        var chords = new KeyChord[] {
                KeyChord.ctrl(GLFW.GLFW_KEY_S),
                KeyChord.ctrlShift(GLFW.GLFW_KEY_S),
                KeyChord.ctrlAlt(GLFW.GLFW_KEY_S),
                KeyChord.alt(GLFW.GLFW_KEY_F4),
                KeyChord.key(GLFW.GLFW_KEY_DELETE),
                KeyChord.key(GLFW.GLFW_KEY_KP_ENTER),
                KeyChord.of(GLFW.GLFW_KEY_TAB, true, true, true),
                KeyChord.UNBOUND,
        };
        for (var chord : chords) {
            assertEquals(chord, KeyChord.parse(chord.serialize()).orElseThrow(),
                    "round trip of " + chord.serialize());
        }
    }

    @Test
    void theTextFormIsTheOneWrittenIntoTheSettingsFile() {
        assertEquals("ctrl+shift+s", KeyChord.ctrlShift(GLFW.GLFW_KEY_S).serialize());
        assertEquals("ctrl+alt+s", KeyChord.ctrlAlt(GLFW.GLFW_KEY_S).serialize());
        assertEquals("delete", KeyChord.key(GLFW.GLFW_KEY_DELETE).serialize());
        assertEquals("kp_add", KeyChord.key(GLFW.GLFW_KEY_KP_ADD).serialize());
        // an unbound action is stored as an empty string, which is how "cleared" is told apart from
        // "never touched" (the entry is absent in that case)
        assertEquals("", KeyChord.UNBOUND.serialize());
    }

    @Test
    void modifierOrderAndCaseDoNotMatterWhenReading() {
        var expected = KeyChord.ctrlShift(GLFW.GLFW_KEY_S);
        assertEquals(expected, KeyChord.parse("shift+ctrl+s").orElseThrow());
        assertEquals(expected, KeyChord.parse("Ctrl+Shift+S").orElseThrow());
        assertEquals(expected, KeyChord.parse(" ctrl + shift + s ").orElseThrow());
        // macOS writes its own modifier names, and a config shared between machines must still load
        assertEquals(expected, KeyChord.parse("cmd+shift+s").orElseThrow());
    }

    @Test
    void unreadableChordsAreRejectedRatherThanGuessed() {
        assertTrue(KeyChord.parse("ctrl+nonsense").isEmpty());
        assertTrue(KeyChord.parse("hyper+s").isEmpty());
        assertTrue(KeyChord.parse("+").isEmpty());
        assertTrue(KeyChord.parse(null).isEmpty());
    }

    @Test
    void aModifierOnItsOwnIsNotAChord() {
        assertFalse(new KeyChord(GLFW.GLFW_KEY_LEFT_SHIFT, 0).isBound());
        assertFalse(new KeyChord(GLFW.GLFW_KEY_LEFT_CONTROL, KeyChord.MOD_CTRL).isBound());
        assertFalse(KeyChord.parse("ctrl").isPresent());
    }

    @Test
    void modifiersOutsideTheThreeAreDropped() {
        var chord = new KeyChord(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_CAPS_LOCK
                | GLFW.GLFW_MOD_NUM_LOCK | GLFW.GLFW_MOD_SUPER);

        assertEquals(KeyChord.ctrl(GLFW.GLFW_KEY_S), chord);
    }

    @Test
    void matchingIsExactAboutModifiers() {
        var save = KeyChord.ctrl(GLFW.GLFW_KEY_S);

        assertTrue(save.matches(GLFW.GLFW_KEY_S, true, false, false));
        // Ctrl+Shift+S is save-as, a different action: a loose match would fire both
        assertFalse(save.matches(GLFW.GLFW_KEY_S, true, true, false));
        assertFalse(save.matches(GLFW.GLFW_KEY_S, false, false, false));
        assertFalse(save.matches(GLFW.GLFW_KEY_A, true, false, false));
        assertFalse(KeyChord.UNBOUND.matches(GLFW.GLFW_KEY_UNKNOWN, false, false, false));
    }

    @Test
    void displayStringsReadLikeTheKeyboard() {
        assertEquals("Ctrl+Shift+S", KeyChord.ctrlShift(GLFW.GLFW_KEY_S).toDisplayString());
        assertEquals("Delete", KeyChord.key(GLFW.GLFW_KEY_DELETE).toDisplayString());
        assertEquals("Num Enter", KeyChord.key(GLFW.GLFW_KEY_KP_ENTER).toDisplayString());
        assertEquals("F5", KeyChord.key(GLFW.GLFW_KEY_F5).toDisplayString());
        assertEquals("", KeyChord.UNBOUND.toDisplayString());
    }

    @Test
    void everyNamedKeyHasADistinctNameAndReadsBack() {
        // the table is written out by hand, so a copy-paste slip that gives two keys the same name (or
        // a name that does not read back) would otherwise surface as a shortcut silently binding another key
        for (int keyCode = 0; keyCode <= GLFW.GLFW_KEY_LAST; keyCode++) {
            var name = KeyNames.nameOf(keyCode);
            if (name == null) continue;
            assertEquals(keyCode, KeyNames.codeOf(name), "name " + name + " reads back to another key");
        }
    }
}
